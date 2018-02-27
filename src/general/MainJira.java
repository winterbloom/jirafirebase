package general;

import categories.comparer.JiraIssueComparer;
import categories.comparer.JiraIssueDelta;
import categories.readers.JiraIssuesReader;
import categories.readers.JiraSprintsReader;
import categories.writers.JiraEpicsWriter;
import categories.writers.JiraIssuesWriter;
import categories.writers.JiraSprintsWriter;
import individuals.IssueOnline;
import individuals.SprintOnline;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static general.JsonParser.kLs;

/*******************************************************
 *
 * Trying to setup some tools for API access to JIRA
 *
 * User: Doug
 * Date: 6/11/2017
 *
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class MainJira {

    /*
     * HOW TO ADD A NEW FIELD:
     * 1) Find the path to the field
     *    Go to https://flowplay.atlassian.net/rest/api/2/search?jql=project%20=%20FLO%20AND%20status%20!=%20DONE%20AND%20status%20!=%20BLOCKED%20AND%20status%20!=%20CANCELED%20AND%20status%20!=%20Closed%20AND%20type%20!=%20Epic&maxResults=1000&expand=renderedFields
     *    Use CTRL+F to search for the name of your field
     *    Copy the "id" field to your clipboard (NOT the name)
     * 2) Make sure you have the field you want
     *    Go to https://flowplay.atlassian.net/rest/api/2/search?jql=project=FLO&fields=[YOUR FIELD HERE]
     *    Make sure you're getting the results you expect
     *    (This is the point where you should notice if you want a sub-field
     *     eg: status, statusCategory, name)
     *    (Also note if it's a string, JSONObject, or JSONArray that you're getting.)
     * 3) Add your field to the path
     *    Not your sub-field. Your FIELD.
     *    Add it to the end of kIssueLink by adding a comma, then the field id
     *
     * THE FOLLOWING IS FOR HOW TO ADD A NEW *STRING* FIELD:
     *
     * 4) Add your field to the Field enum in Fields
     *    Here, don't use the id. Use the actual name, since this should be human-readable
     *    Where you put it is important; this is the order the fields are sent in on the emails
     * 4.5) Add your field to the defaults HashMap in Fields
     *    Find what the field defaults to and fill it in
     * 5) Add the path to IssueJira
     *    Add this line to the constructor:
     *    fieldsMap.put(Field.[YOUR FIELD HERE], JsonParser.safe[SOMETHING](m_fieldsJson, [YOUR PATH]));
     *    So if you have id - created, name - creationDate, path - created you'd add
     *    fieldsMap.put(Field.creationDate, JsonParser.safeGet(m_fieldsJson, "created"));
     *    And if you have id - status, name - status, path - status.statusCategory.name
     *    fieldsMap.put(Field.status, JsonParser.safeSubChild(m_fieldsJson, "status", "statusCategory", "name"));
     * 6) (optional, but recommended) Repeat steps 1 - 3 and 5, but for epics
     *    (The url you want to change is kEpicLink)
     *    (The class you want is JiraEpicsWriter, not IssueJira. Otherwise, it's the same
     *    (If your field isn't applicable, set it to kNA)
     * 7) If you want to make your field a link in an email:
     *    Go to EmailSender; mimic every instance of Field.epicLink.
     *    You'll figure it out :)
     *
     * THE FOLLOWING IS FOR HOW TO ADD A NEW *NOT STRING* FIELD;
     * 4) ...Don't.
     * 5) Run a usage search on every time anything related to watchers/comments appears.
     *    Implement yours wherever they're implemented.
     *    Good luck
     */

    public final static String kIssues = "issues";
    public final static String kEpics = "epics";
    public final static String kSprints = "sprints";
    public final static String kCurrentSprintIssues = "current-sprint";
    public final static String kArtSprintIssues = "art-sprint";

    static File output;
    static File changes;
    static File lockFile ;
    static File rawJSON ;

    private static boolean sendEmails = false ;
    private static boolean writeToFirebase = false ;
    private static String testOneIssue = null ; // If defined we explicitly request data for this one issue - for debugging

    private enum Mode {kEpicMode, kSprintMode, kIssueMode, kCurrentSprintIssues, kArtSprintIssues, kRepopulateFirebase, kReadFromFirebase}
    //Epic - write/read from epic data; sprint; issue
    private static Mode currIssueMode;
    private static boolean epicM() { return currIssueMode == Mode.kEpicMode; }
    private static boolean sprintM() { return currIssueMode == Mode.kSprintMode; }
    private static boolean issueM() { return currIssueMode == Mode.kIssueMode; }
    private static boolean currentM() { return currIssueMode == Mode.kCurrentSprintIssues; }
    private static boolean artM() { return currIssueMode == Mode.kArtSprintIssues; }

    private static boolean readIssuesM() { return issueM() || currentM() || artM() ; }

    /* Create firebase - sends no emails, does not read from firebase,
     *   simply writes JIRA data to Firebase
     * Read from firebase - reads Firebase data, reads JIRA data, sends emails,
     *   clears Firebase and writes new JIRA data
     */
    private static Mode currFirebaseMode = Mode.kReadFromFirebase;
    private static boolean createM() { return currFirebaseMode == Mode.kRepopulateFirebase; }
    private static boolean readM() { return currFirebaseMode == Mode.kReadFromFirebase; }

    public static final String kEpicLink = "https://flowplay.atlassian.net/rest/api/2/search?" +
            "jql=project=FLO%20AND%20issuetype%20=%20Epic%20AND%20status%20!=%20CANCELED%20AND%20status%20!=%20Closed%20AND%20status%20!=%20DONE%20AND%20status%20!=%20RESOLVED&" +
            "maxResults=1000&expand=renderedFields&fields=created,priority,status,summary,watches,reporter,comment," +
            "customfield_10115,customfield_10206,customfield_10607,customfield_10700,customfield_10602,issuetype";

    private static final String kFieldsURL =
            "expand=renderedFields&fields=summary,priority,assignee,status,created,reporter,watches,description," +
            "customfield_10115,customfield_10700,customfield_10204,customfield_10205,customfield_10206,customfield_10603,customfield_10802,customfield_10604," +
            "customfield_10607,customfield_10005,customfield_10602,customfield_10117,customfield_10900,comment,updated,issuetype";


    public static final String kIssueLink = "https://flowplay.atlassian.net/rest/api/2/search?" +
            "jql=project%20=%20FLO%20AND%20status%20!=%20DONE%20AND%20status%20!=%20CANCELED%20AND%20status%20!=%20Closed%20AND%20" +
            "type%20!=%20Epic&maxResults=1000&" +
            kFieldsURL ;

    //(openSprints(), futureSprints()) AND Sprint not in (20) - 20 is icebox
    public static final String kCurrentSprintLink = "https://flowplay.atlassian.net/rest/api/2/search?" +
            "jql=Sprint%20in%20(openSprints(),futureSprints())%20AND%20Sprint%20not%20in%20(20)%20AND%20" +
            //"assignee%20not%20in%20membersOf(Artists)%20AND%20" +
            "type%20!=%20Epic&maxResults=1000&" +
            kFieldsURL ;

    public static final String kArtSprintLink = "https://flowplay.atlassian.net/rest/api/2/search?" +
            "jql=Sprint%20in%20(openSprints(),futureSprints())%20AND%20Sprint%20not%20in%20(20)%20AND%20" +
            "assignee%20in%20membersOf(Artists)%20AND%20" +
            "type%20!=%20Epic&maxResults=1000&" +
            kFieldsURL ;

    /* The flowplay scrum board's url is:
    flowplay.atlassian.net/secure/RapidBoard.jspa?rapidView=6&quickFilter=24
    The 6 in the following URL is because I'm fetching the sprints from the board with id 6 */
    public static final String kSprintLink = "https://flowplay.atlassian.net/rest/greenhopper/1.0/sprintquery/6?includeFutureSprints=true&includeHistoricSprints=false";

    private static boolean parseArgs(String args[]) {
        boolean type = false;
        boolean out = false;
        boolean in = false;
        boolean mode = false ;

        for (String arg : args) {
            if (arg.substring(0, 1).equals("-")) { //If it begins with a dash (ie: a tag)
                String tag = arg.substring(1, arg.length());
                switch (tag) {
                    case "repopulate":
                        currFirebaseMode = Mode.kRepopulateFirebase;
                        break;
                }
            } else {
                int equals = arg.indexOf("=");
                if (equals != -1){ //If it has an equals (ie: a parameter)
                    String tag = arg.substring(0, equals);
                    String param = arg.substring(equals + 1, arg.length());
                    switch (tag) {
                        case "type":
                            switch (param) {
                                case "epic":
                                    currIssueMode = Mode.kEpicMode;
                                    type = true;
                                    break;
                                case "sprint":
                                    currIssueMode = Mode.kSprintMode;
                                    type = true;
                                    break;
                                case "issue":
                                    currIssueMode = Mode.kIssueMode;
                                    type = true;
                                    break;
                                case "current-sprint":
                                    currIssueMode = Mode.kCurrentSprintIssues;
                                    type = true;
                                    break;
                                case "art-sprint":
                                    currIssueMode = Mode.kArtSprintIssues;
                                    type = true;
                                    break;
                            }
                            break;
                        case "output":
                            output = new File (param, "output.txt");
                            changes = new File (param, "changes.txt");
                            lockFile = new File (param, "lockfile.txt") ;
                            rawJSON = new File (param, "raw-json.json") ;
                            out = true;
                            break;
                        case "input":
                            DatabaseOnline.kSAccountURL = new File(param, "admin-account.json");
                            in = true;
                            break;
                        case "mode" :
                            switch (param) {
                                case "execute":
                                    sendEmails = true ;
                                    writeToFirebase = true ;
                                    mode = true ;
                                    break;
                                case "test":
                                    sendEmails = false ;
                                    writeToFirebase = false ;
                                    mode = true ;
                                    break;
                                default:
                                    // mode=FLO-1234 is now allowed so
                                    if (param.startsWith("FLO")) {
                                        testOneIssue = param ;
                                        mode = true ;
                                        sendEmails = false ;
                                        writeToFirebase = false ;
                                    }
                                    break ;
                            }
                            break ;
                    }
                }
            }
        }
        if (!type) { //No type parameter was added
            System.out.println("usage: type= either \"epic\", \"sprint\", or \"issue\" for what type of data to handle.");
        }
        if (!out) {
            System.out.println("usage: output= where the output files should be stored");
        }
        if (!in) {
            System.out.println("usage: input= where the input files should be read from");
        }
        if (!type && !out && !in) { //No parameters were included
            System.out.println("usage: -repopulate (optional) sends no emails, doesn't read from firebase, just writes JIRA data to Firebase. WARNING: this is VERY slow (about 7 mins).");
        }
        if (!mode) {
            System.out.println("usage: mode=execute | test.  In test mode sends no emails and does not write to firebase") ;
        }
        return type && out && in && mode ;
    }

    public static void main(String args[]) {
        try {
            if (!parseArgs(args)) {
                return;
            }

            if (currentM() || artM()) {
                updateFirebaseFromJiraCurrentSprintOnly() ;
            } else {
                updateFirebaseFromJiraAndSendEmail() ;
            }
        } catch (Exception ex) {
            System.err.println("Exception at top level") ;
            ex.printStackTrace();
            System.exit(1); // We have extra threads running so need to exit
        }
    }

    private static void updateFirebaseFromJiraAndSendEmail() {
        System.out.println("1) Starting JiraFirebase at " + new Date()) ;

        MainJira app = new MainJira() ;
        app.checkLockFile(lockFile) ;

        // Clear output file
        // Removed this so now always appends to the output
        /*
        if (readM()) { writeToFile("", output, false); }
        if (readM()) { writeToFile("", changes, false); }
        */

        // Append the date to the changes log
        if (readM()) {
            writeToFile("==== Scanning at " + new Date() + " ====\n", changes, true, "Scanning");
        }

        app.readFromFirebase();

        //Jira Readers
        Map<String, ?> jira = app.readFromJira(kIssueLink) ;

        System.out.println("2) Finished reading from Firebase and Jira at " + new Date()) ;

        //Get delta between Firebase (old data) and Jira (new data)
        List<JiraIssueDelta> deltas = app.compareCurrentJiraToFirebase(jira);

        System.out.println("2b) Finished comparisons at " + new Date() + " with " + deltas.size() + " changes") ;

        if (createM() || deltas.size() != 0) { //If anything's changed, or you're in a mode where you don't look for deltas :)
            System.out.println("2c) About to write to Firebase at " + new Date()) ;

            app.writeToFirebase(jira) ;

            System.out.println("3) Finished writing to Firebase at " + new Date()) ;

            app.sendEmails(deltas);

            System.out.println("4) Finished sending emails at " + new Date()) ;

            if (writeToFirebase) {
                //Firebase Clearer (removes the earlier set of issues from the database)
                DatabaseOnline.clear();

                //Wait for the clearing to finish
                sleep(10);
            }

            System.out.println("4a) Start writing deltas at " + new Date()) ;

            if (readM()) {
                for (JiraIssueDelta delta : deltas) {
                    System.out.println("4b) Checking delta in " + delta.getID()) ;
                    if (!delta.areEqual()) {
                        writeToFile(delta.toString() + kLs + kLs, changes, true, "Delta to " + delta.getID());
                    }
                }
            }
        }

        System.out.println("4c) Time to remove lock file at " + new Date()) ;

        app.removeLockFile(lockFile);

        System.out.println("5) Finished JiraFirebase at " + new Date()) ;

        System.exit(0);
    }

    private static void updateFirebaseFromJiraCurrentSprintOnly() {
        String title = currentM() ? "JiraCurrentSprint" : "JiraArtSprint" ;
        System.out.println("1) Starting " + title + " at " + new Date()) ;

        // Don't load full details about the issues
        IssueOnline.kFastIssue = true ;

        MainJira app = new MainJira() ;
        app.checkLockFile(lockFile) ;

        app.readFromFirebase();

        String jsql = currentM() ? kCurrentSprintLink : kArtSprintLink ;

        //Jira Readers
        Map<String, ?> jira = app.readFromJira(jsql) ;

        System.out.println("2) Finished reading from Firebase and Jira at " + new Date()) ;

        System.out.println("2a) About to write to Firebase at " + new Date()) ;

        app.writeToFirebase(jira) ;

        System.out.println("3) Finished writing to Firebase at " + new Date()) ;

        if (writeToFirebase) {
            //Firebase Clearer (removes the earlier set of issues from the database)
            DatabaseOnline.clear();

            //Wait for the clearing to finish
            sleep(10);
        }

        System.out.println("4) Time to remove lock file at " + new Date()) ;

        app.removeLockFile(lockFile);

        System.out.println("5) Finished JiraCurrentSprint at " + new Date()) ;

        System.exit(0);
    }

    private void checkLockFile(File lockFile) {
        try {
            if (lockFile.exists()) {
                System.out.println("Lock file found at " + lockFile + " so aborting") ;

                EmailSender.sendEmail("doug@flowplay.com", "JiraFirebase lock file still exists",
                        "Lock file " + lockFile.getCanonicalPath() + " exists which means JiraFirebase tool is still running.<br>" +
                                "This can be OK if it only happens once in a while - it likely means that the last run took over 5 mins.<br>" +
                                "But if it keeps happening, is there a left over jirafirebase process running on reports?");

                // Quick right away
                System.exit(1);
            }

            writeToFile("Lock file for JiraFirebase " + new Date(), lockFile, false, "Creating lock file");
        } catch (Exception ex) {
            throw new IllegalStateException(ex) ;
        }
    }

    private void removeLockFile(File lockFile) {
        lockFile.delete() ;
    }

    private void readFromFirebase() {
        //Firebase Initializers
        if (epicM()) { DatabaseOnline.initialize(MainJira.kEpics); }
        if (sprintM()) { DatabaseOnline.initialize(MainJira.kSprints); }
        if (issueM()) { DatabaseOnline.initialize(MainJira.kIssues); }
        if (currentM()) { DatabaseOnline.initialize(MainJira.kCurrentSprintIssues); }
        if (artM()) { DatabaseOnline.initialize(MainJira.kArtSprintIssues); }

        //Firebase Readers
        if (readM()) {
            if (epicM()) { JiraIssuesReader.readIssues(); }
            if (sprintM()) { JiraSprintsReader.readSprints(); }
            if (issueM()) { JiraIssuesReader.readIssues(); }
        }
    }

    private Map<String, ?> readFromJira(String jsqlURL) {
        //Jira Readers
        HashMap<String, ?> jira = new HashMap<String, IssueOnline>();
        if (epicM()) { jira = JiraEpicsWriter.getEpics(); }
        if (sprintM()) { jira = JiraSprintsWriter.getSprints(); }
        if (readIssuesM()) { jira = JiraIssuesWriter.getJiraIssues(jsqlURL, testOneIssue); }
        return jira ;
    }

    private List<JiraIssueDelta> compareCurrentJiraToFirebase(Map<String, ?> jira) {
        //Get delta between Firebase (old data) and Jira (new data)
        ArrayList<JiraIssueDelta> deltas = new ArrayList<>();
        if (readM()) {
            deltas = JiraIssueComparer.getDeltas(JiraIssuesReader.getIssues(), (HashMap<String, IssueOnline>) jira);
        }
        return deltas ;
    }

    private void writeToFirebase(Map<String, ?> jira) {
        if (writeToFirebase) {
            //Firebase Writers
            if (epicM()) {
                JiraEpicsWriter.refreshFirebaseEpics((HashMap<String, IssueOnline>) jira);
            }
            if (sprintM()) {
                JiraSprintsWriter.refreshFirebaseSprints((HashMap<String, SprintOnline>) jira);
            }
            if (readIssuesM()) {
                JiraIssuesWriter.refreshFirebaseIssues((HashMap<String, IssueOnline>) jira);
            }

            //Update the version number to the next version
            DatabaseOnline.updateVersion();
        }
    }

    private void sendEmails(List<JiraIssueDelta> deltas) {
        if (readM()) {
            // Adding an automatic safety valve
            // so if we think the whole database has changed (e.g. due to a new field being added or a change to the way data is represented)
            // we never send out a massive flood of emails
            if (deltas.size() >= 100) {
                System.out.println("Found " + deltas.size() + " changes which is too many to send emails about - this is probably a complete database reset") ;
                return;
            }

            //Send the emails from the deltas
            EmailConfig config = new EmailConfig(sendEmails);
            config.addSendMyChangesToMe("doug@flowplay.com", true);

            // For now send all bugs email to just doug
            // config.mapEmailAddressToNewAddress("bugs@flowplay.com", "doug@flowplay.com");

            EmailSender.sendEmailsFromDeltas(deltas, config);
        }
    }

    //curl -D- -u <user>:<password> -X GET -H "Content-Type: application/json" https://flowplay.atlassian.net/rest/api/2/search?jql=assignee=admin
    public static String getDataFromURL(String url) {
        try {
            HttpHost host = new HttpHost("flowplay.atlassian.net", 443, "https");
            Executor executor = Executor.newInstance()
                    .auth(host, "user", "password").authPreemptive(host);

            String line = executor.execute(Request.Get(url).
                    addHeader("Content-Type", "application/json").
                    connectTimeout(120 * 1000).socketTimeout(120 * 1000)).returnContent().asString();
            return line;
        } catch (HttpResponseException rex) {
            int statusCode = rex.getStatusCode() ;
            System.err.println("Status code was " + statusCode) ;
            rex.printStackTrace();
            return null ;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null ;
        }
    }

    /**
     * Sleeps the thread for the given amount, with a little waiting animation
     * @param seconds The time to sleep for, in seconds
     */
    private static void sleep(long seconds) {
        try {
            System.out.println(kLs + "Sleeping for " + seconds + " seconds:");
            for (int i = 0; i < seconds * 2 - 1; i++) {
                System.out.print("_");
            }
            System.out.println();

            for (int i = 0; i < seconds; i++) {
                Thread.sleep(1000);
                System.out.print(". ");
            }
            System.out.println();
        } catch (InterruptedException ex) {
            System.err.println("Exception while trying to sleep: ");
            ex.printStackTrace();
        }
    }

    /**
     * Writes the given text to the given file, with optional appending or overwriting
     * @param text The text to write
     * @param targetFile The file to write to
     * @param append True to append to the receivingBecause of the file; false to overwrite the file's contents
     */
    static void writeToFile(String text, File targetFile, boolean append, String logMsg) {
        if (append) { System.out.println("Writing to " + targetFile + " " + logMsg + " at " + new Date()) ; }
        else if (text.equals("")) { System.out.println("Clearing " + targetFile); }
        else { System.out.println("Overwriting " + targetFile); }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(targetFile, append));
            writer.write(text);
            writer.close();
        } catch (IOException ex) {
            System.err.print("IOException caught while trying to write to output file.");
            ex.printStackTrace();
        }
    }

    public static void writeRawJSON(String json, boolean append, String logMsg) {
        // Adding a couple of line feeds so easy to see breaks if multiple JSON blobs in one file
        writeToFile(json + kLs + kLs, rawJSON, append, logMsg);
    }
}
