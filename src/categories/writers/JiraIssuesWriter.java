package categories.writers;

import general.DatabaseOnline;
import general.Fields;
import general.MainJira;
import individuals.IssueOnline;
import individuals.IssuesJira;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains the code for pulling issue data
 *
 * User: Hazel
 * Date: 8/10/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraIssuesWriter {

    /**
     * Reads from JIRA's issues and pushes the data to firebase
     */
    public static void refreshFirebaseIssues(HashMap<String, IssueOnline> issues) {
        if (issues != null) {
            System.out.println("Writing data to firebase " + issues.size() + " issues");
            for (IssueOnline curr : issues.values()) {
                //System.out.print(curr.getField(Fields.Field.id) + ", ");
                DatabaseOnline.write(curr, curr.getField(Fields.Field.id));
            }
            //System.out.println();
        }
    }

    /**
     * Reads from JIRA and returns the issues retrieved
     * @return A list of the issues on JIRA
     */
    public static HashMap<String, IssueOnline> getJiraIssues(String jsql, String testOneIssue) {
        System.out.println("Reading issues from JIRA " + jsql);

        if (testOneIssue != null) {
            System.out.println("Focusing on JIRA issue " + testOneIssue);
        }

        IssuesJira issues = parseIssues(jsql, testOneIssue) ;

        if (issues != null) {
            if (issues.size() == 0) {
                System.out.println("No new issues.");
                return null;
            }

            HashMap<String, IssueOnline> issueList = issues.getIssues();

            return issueList;
        }
        return null;
    }

    /**
     * Given a piece of JSON, turns it into an object
     * @param originLink A link to pull JSON data from
     * @param testOneIssue If not null, should be id of an issue (e.g. FLO-1234) that we wish to test explicitly
     * @return An object with the JSON data
     */
    public static IssuesJira parseIssues(String originLink, String testOneIssue) {
        try {
            //There's a cap on how many issues you can pull at once (1000).
            //This expands that cap
            int totalToGet = 0 ;
            int resultsRetrieved;
            int maxPerPage = 1000 ;     // They may reduce this when we make the request
            JSONArray jsonIssues = new JSONArray();
            int startAt = 0 ;
            boolean first = true ;
            do {
                //Pull the correct thousand-set
                String url = originLink + "&startAt=" + startAt;
                String line = MainJira.getDataFromURL(url);

                if (line == null) {
                    System.out.println("Had to break out of parseIssues loop at line was null") ;
                    break;
                }

                JSONObject json = new JSONObject(line);
                //See if it's the last set (ie: pulled less than a thousand results)
                totalToGet = json.getInt("total");
                maxPerPage = json.getInt("maxResults");

                startAt += maxPerPage ;

                //Add the set to the array
                JSONArray newJsonIssues = json.getJSONArray(MainJira.kIssues);
                jsonIssues.put(newJsonIssues);

                resultsRetrieved = newJsonIssues.length() ;
                //System.out.println("JIRA returned " + resultsRetrieved + " start " + startAt + " max " + maxPerPage + " total " + totalToGet + " from " + url) ;

                // Record the raw JSON that we read in a file
                MainJira.writeRawJSON(json.toString(2), !first, "raw issues json");
                first = false ;
            }
            while (resultsRetrieved >= maxPerPage);

            IssuesJira issues = IssuesJira.fromIssues(jsonIssues, testOneIssue);
            return issues;
        } catch (JSONException ex) {
            System.err.println("Error parsing JSON: ");
            ex.printStackTrace();
            return null;
        }
    }
}
