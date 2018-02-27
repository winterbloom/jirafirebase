package individuals;

import categories.readers.JiraIssuesReader;
import general.Fields;
import general.Fields.Field;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 *
 *
 * User: Doug
 * Date: 6/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class IssuesJira {
    private final HashMap<String, IssueOnline> m_issues ;

    IssuesJira(HashMap<String, IssueOnline> issues) {
        m_issues = issues;
    }

    public static IssuesJira fromIssues(JSONArray json, String testOneIssue) throws JSONException {
        HashMap<String, IssueOnline> issues = new HashMap<>() ;

        HashMap<String, IssueOnline> currFirebaseIssues = JiraIssuesReader.getIssues();

        for (int i = 0 ; i < json.length() ; i++) {
            JSONArray jArr = json.getJSONArray(i) ;

            for (int k = 0; k < jArr.length(); k += 50) {
                System.out.print("__");
            }
            System.out.println();
            for (int j = 0; j < jArr.length(); j++) {
                if (j % 50 == 0) { System.out.print(". "); }

                JSONObject jObj = jArr.getJSONObject(j);
                IssueJira issue = new IssueJira(jObj);
                HashMap<Field, String> fieldsMap = issue.getFieldsMap();
                String id = fieldsMap.get(Fields.Field.id);
                String lastUpdated = fieldsMap.get(Field.lastUpdated);

                boolean testThisIssue = false ;
                if (testOneIssue != null && id.equalsIgnoreCase(testOneIssue))
                    testThisIssue = true ;

                //Read the old info from firebase
                IssueOnline currFirebaseIssue = currFirebaseIssues.get(id);
                //Read the last-updated time stamp on the old firebase data
                // And the time stamp on the new jira data
                // If they're different or the one in firebase doesn't exist,
                // do all this work, else, doesn't matter.
                if (currFirebaseIssue == null || testThisIssue ||
                        !lastUpdated.equals(currFirebaseIssue.getField(Field.lastUpdated))) {
                    IssueOnline iss = new IssueOnline(issue);
                    issues.put(id, iss);
                } else {
                    //Firebase still needs the data
                    issues.put(currFirebaseIssue.getField(Fields.Field.id), currFirebaseIssue);
                }
            }
        }

        System.out.println();
        System.out.println("Done reading from JIRA");
        return new IssuesJira(issues) ;
    }

    public HashMap<String, IssueOnline> getIssues() {
        return m_issues;
    }

    public int size() {
        return m_issues.size() ;
    }

    @Override
    public String toString() {
        return "JiraIssues{" +
                m_issues +
                '}';
    }
}
