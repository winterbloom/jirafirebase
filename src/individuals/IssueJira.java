package individuals;

import general.Fields;
import general.Fields.Field;
import general.JsonParser;
import general.MainJira;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static general.Fields.Field.id;
import static general.MainJira.getDataFromURL;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/*********************************************************
 * Description:
 *
 * Represents an issue in JIRA
 *
 * User: Doug
 * Date: 6/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class IssueJira {
    private final JSONObject m_totalJson ;
    private final JSONObject m_fieldsJson ;

    private HashMap<Field, String> fieldsMap = new HashMap<>();

    public IssueJira(JSONObject json) throws JSONException {
        m_totalJson = json ;
        m_fieldsJson = m_totalJson.getJSONObject("fields") ;
        fieldsMap.put(Fields.Field.summary, JsonParser.safeGet(m_fieldsJson,"summary"));
        fieldsMap.put(Fields.Field.priority, JsonParser.safeChild(m_fieldsJson,"priority","name"));
        fieldsMap.put(Fields.Field.assignee, JsonParser.safeChild(m_fieldsJson,"assignee","emailAddress"));
        fieldsMap.put(Fields.Field.status, JsonParser.safeChild(m_fieldsJson,"status", "name"));
        fieldsMap.put(Fields.Field.creationDate, Fields.humanReadable(JsonParser.safeGet(m_fieldsJson,"created")));
        fieldsMap.put(Fields.Field.reporter, JsonParser.safeChild(m_fieldsJson,"reporter","emailAddress"));
        fieldsMap.put(Fields.Field.lastUpdated, Fields.humanReadable(JsonParser.safeGet(m_fieldsJson, "updated")));
        fieldsMap.put(Fields.Field.severity, getCustomField("customfield_10204"));
        fieldsMap.put(Fields.Field.hardware, getCustomField("customfield_10205"));
        fieldsMap.put(Fields.Field.os, getCustomField("customfield_10206"));
        fieldsMap.put(Fields.Field.patchIntoPlaytest, getCustomField("customfield_10603"));
        fieldsMap.put(Fields.Field.html5Bug, getCustomField("customfield_10802"));
        fieldsMap.put(Fields.Field.hasBeenPatched, getCustomField("customfield_10604"));
        fieldsMap.put(Fields.Field.fixBeforeHtml5, getCustomField("customfield_10607"));
        fieldsMap.put(Fields.Field.product, getCustomField("customfield_10700"));
        fieldsMap.put(Fields.Field.epicLink, JsonParser.safeGet(m_fieldsJson,"customfield_10005"));
        fieldsMap.put(Fields.Field.world, getCustomField("customfield_10602"));
        fieldsMap.put(Fields.Field.sprint, getSprint());
        fieldsMap.put(Fields.Field.sprints, getSprints());
        fieldsMap.put(id, IssueJira.getID(m_fieldsJson));
        fieldsMap.put(Fields.Field.url, "https://flowplay.atlassian.net/browse/" + fieldsMap.get(id));
        fieldsMap.put(Fields.Field.days, JsonParser.safeGet(m_fieldsJson,"customfield_10117"));
        fieldsMap.put(Field.issueType, JsonParser.safeChild(m_fieldsJson, "issuetype","name"));
        fieldsMap.put(Field.description, JsonParser.safeChild(m_totalJson, "renderedFields","description"));
        fieldsMap.put(Fields.Field.productOwner, JsonParser.safeChild(m_fieldsJson, "customfield_10900", "emailAddress"));
    }

    public HashMap<Field, String> getFieldsMap() { return fieldsMap; }

    private static Date convertJIRADate(String dateStr) {
        // Dates from JIRA look like this:
        // String date = "2017-09-26T20:14:57.473-0700"

        // ISO standard dates look like this:
        // String date = "2017-09-26T20:14:57.473-07:00"     <== 07:00 note the :

        // That causes lots of problems so we need a special parser...
        DateTimeFormatter jiraDateFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE_TIME)
                .appendOffset("+HHmm", "+00:00")
                .toFormatter();

        Date date = Date.from( ZonedDateTime.parse( dateStr , jiraDateFormat ).toInstant() ) ;
        return date ;
    }

    // These don't seem to come in a reliable order
    // so we'll search through the list to find the one that is most recent
    private static JSONObject findMostRecentChange(JSONArray changelog) throws JSONException {
        JSONObject lastChange = null ;
        Date lastDate = null ;

        for (int i = 0 ; i < changelog.length() ; i++) {
            JSONObject nextChange = changelog.getJSONObject(i);

            String createdDateStr = nextChange.getString("created");
            Date createdDate = convertJIRADate(createdDateStr);

            if (lastChange == null || lastDate == null || createdDate.after(lastDate)) {
                lastChange = nextChange ;
                lastDate = createdDate ;
            }
        }

        return lastChange ;
    }

    public static String getChangedBy(String id, String reporter, Map<String, String> lastComment) {
        String changedBy = "";
        try {
            JSONObject data = new JSONObject(getDataFromURL("https://flowplay.atlassian.net/rest/api/2/issue/" + id + "?expand=changelog"));
            JSONArray changelog = data.getJSONObject("changelog").getJSONArray("histories");
            if (changelog.length() != 0) {
                JSONObject lastChange = findMostRecentChange(changelog) ;

                String createdDateStr = lastChange.getString("created") ;
                Date createdDate = convertJIRADate(createdDateStr) ;

                changedBy = lastChange.getJSONObject("author").getString("emailAddress");

                // Comments unfortunately do not show up on the change list
                // so we need to consider who left the last comment and if that
                // was later, then that was the last person to change this bug
                if (lastComment != null) {
                    String lastCommentCreatedDateStr = lastComment.get("created") ;
                    String commentAuthor = lastComment.get("author") ;
                    Date lastCommentDate = convertJIRADate(lastCommentCreatedDateStr) ;

                    if (lastCommentDate.after(createdDate)) {
                        System.out.println("Last comment for " + id + " was " + lastCommentDate + " which is after last change " + createdDate + " so using last commenter " + commentAuthor + " not " + changedBy) ;
                        changedBy = commentAuthor ;
                    } else {
                        System.out.println("Last comment for " + id + " was not after last change so using last changer " + changedBy) ;
                    }
                } else {
                    System.out.println("No last comment for " + id + " so last changed " + changedBy) ;
                }

            } else { //No one's changed this issue, it's only been created
                if (lastComment != null) {
                    String commentAuthor = lastComment.get("author") ;
                    changedBy = commentAuthor ;
                    System.out.println("No changes for " + id + " and last commenter was " + commentAuthor);
                } else {
                    System.out.println("No changes for " + id + " so using reporter " + reporter);
                    changedBy = reporter;
                }
            }
        } catch (JSONException ex) {
            System.err.println("Error trying to retrieve who last changed issue " + id);
            ex.printStackTrace();
        }

        System.out.println("Got changed by for " + id + " " + changedBy) ;

        return changedBy;
    }

    public ArrayList<String> getWatchers() {
        return IssueJira.getWatchers(fieldsMap.get(id));
    }

    public static ArrayList<String> getWatchers(String id) {
        ArrayList<String> watchersList = new ArrayList<>();
        try {
            String jsonWatchers = MainJira.getDataFromURL("https://flowplay.atlassian.net/rest/api/2/issue/" + id + "/watchers") ;
            
            JSONObject data = new JSONObject(jsonWatchers);
            JSONArray watchers = data.getJSONArray("watchers");
            for (int i = 0; i < watchers.length(); i++) {
                JSONObject curr = watchers.getJSONObject(i);
                watchersList.add(JsonParser.safeGet(curr, "emailAddress"));
            }
        } catch (Exception ex) {
            System.err.println("Error trying to retrieve watchers for " + id);
            ex.printStackTrace();
            throw new IllegalStateException(ex) ;
        }

        System.out.println("Got watchers for " + id + " " + watchersList) ;

        return watchersList;
    }

    public ArrayList<HashMap<String, String>> getComments() {
        return IssueJira.getComments(m_totalJson, fieldsMap.get(id), fieldsMap.get(Field.issueType));
    }

    public static ArrayList<HashMap<String, String>> getComments(JSONObject totalJson, String bugId, String issueType) {
        ArrayList<HashMap<String, String>> commentList = new ArrayList<>();
        try {
            // There seem to be two representation of comments
            // one more human readable and one more machine readable
            // Both are potentially useful to us
            JSONArray rawComments = totalJson.getJSONObject("fields").getJSONObject("comment").getJSONArray("comments");
            JSONArray comments = totalJson.getJSONObject("renderedFields").getJSONObject("comment").getJSONArray("comments");
            for (int i = 0; i < comments.length(); i++) {
                JSONObject currComment = comments.getJSONObject(i);
                JSONObject currRawComment = rawComments.getJSONObject(i);

                HashMap<String, String> newComment = new HashMap<>();
                newComment.put("author", JsonParser.safeChild(currComment,"author","emailAddress"));
                newComment.put("body",JsonParser.safeGet(currComment,"body"));
                String id = JsonParser.safeGet(currComment,"id");
                newComment.put("id",id);
                newComment.put("parentBugId",bugId);
                newComment.put("parentIssueType",issueType);

                String createdStr = JsonParser.safeGet(currRawComment, "created") ;
                newComment.put("created", createdStr) ;

                commentList.add(newComment);
            }

        } catch (JSONException ex) {
            System.err.println("Error trying to retrieve comments: ");
            ex.printStackTrace();
        }

        //System.out.println("Got comments for " + bugId + " " + commentList.size()) ;

        return commentList;
    }

    private List<String> getAllSprints() {
        List<String> sprints = new ArrayList<>() ;

        if (!m_fieldsJson.isNull("customfield_10115")) {
            String data = JsonParser.safeGet(m_fieldsJson, "customfield_10115");

            /* [
            "com.atlassian.greenhopper.service.sprint.Sprint@1e49e9ba[id=23,rapidViewId=2,state=CLOSED,name=6/26 Sprint,goal=,startDate=2017-06-26T18:30:40.259Z,endDate=2017-07-08T01:00:00.000Z
                    ,completeDate=2017-07-10T18:35:58.410Z,sequence=22]",
            "com.atlassian.greenhopper.service.sprint.Sprint@5242d133[id=24,rapidViewId=6,state=CLOSED,name=7/10 Sprint (24),goal=,startDate=2017-07-10T18:30:44.797Z,endDate=2017-07-22T01:00:00
            .000Z,completeDate=2017-07-24T21:08:30.287Z,sequence=23]",
            "com.atlassian.greenhopper.service.sprint.Sprint@39d21748[id=31,rapidViewId=6,state=FUTURE,name=10/16 Sprint (31),goal=,startDate=<null>,endDate=<null>,completeDate=<null>,sequence=
            30]
            */
            boolean done = false ;
            int pos = 0 ;

            // Find the ",name=" entries and pull the value following that.
            while (!done) {
                pos = data.indexOf(",name=", pos) ;

                if (pos == -1) {
                    done = true ;
                    continue;
                }

                pos += ",name=".length() ;

                int end = data.indexOf(",", pos) ;
                String sprint = data.substring(pos, end) ;
                sprints.add(sprint) ;

                pos = end ;
            }
        }

        // Sort them in order of sprint number
        sprints.sort(new SprintSorter());

        return sprints ;
    }

    private String getSprints() {
        List<String> all = getAllSprints() ;
        return all.toString() ;
    }

    private static class SprintSorter implements Comparator<String> {
        // [11/13 Sprint (33) <= find the 33
        private static int getSprintNumber(String sprint) {
            int open = sprint.indexOf("(") ;
            int close = sprint.indexOf(")") ;
            if (open == -1 || close == -1 || open > close)
                return 0 ;

            String number = sprint.substring(open+1, close) ;
            int sprintNumber = Integer.parseInt(number) ;
            return sprintNumber ;
        }

        @Override
        public int compare(String o1, String o2) {
            int sprint1 = getSprintNumber(o1) ;
            int sprint2 = getSprintNumber(o2) ;

            return Integer.compare(sprint1, sprint2) ;
        }
    }

    public String getSprint() {
        List<String> all = getAllSprints() ;

        if (all.isEmpty())
            return "" ;

        // Return the last sprint
        return all.get(all.size()-1) ;
    }

    private String getCustomField(String customField) {
        try {
            if (m_fieldsJson.has(customField) && !m_fieldsJson.isNull(customField)) {
                JSONObject data = m_fieldsJson.getJSONObject(customField);

                return data.getString("value");
            }
        } catch (JSONException ex) {
            System.err.println("Error trying to get custom field " + customField + " from " + m_fieldsJson + ":");
            ex.printStackTrace();
            return null;
        }
        return "";
    }

    public static String getID(JSONObject issue) {
        String watchers = JsonParser.safeChild(issue,"watches","self");
        //eg "https://flowplay.atlassian.net/rest/api/2/issue/FLO-2693/watchers"

        int flo = watchers.indexOf("FLO-");
        int slash = watchers.indexOf("/",flo);
        if (slash != -1) {
            return watchers.substring(flo, slash);
        } else {
            return watchers.substring(flo);
        }
    }

    @Override
    public String toString() {
        return "JiraIssue{" + "m_fieldsJson=\n" + m_fieldsJson + "\n" ;
    }
}
