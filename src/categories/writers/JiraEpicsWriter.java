package categories.writers;

import general.DatabaseOnline;
import general.Fields;
import general.JsonParser;
import general.MainJira;
import individuals.IssueJira;
import individuals.IssueOnline;
import general.Fields.Field;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains the code for pulling epic data
 *
 * User: Hazel
 * Date: 8/10/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraEpicsWriter {

    public static void refreshFirebaseEpics(HashMap<String, IssueOnline> epics) {
        if (epics != null) {
            for (IssueOnline curr : epics.values()) {
                System.out.println(curr);
                DatabaseOnline.write(curr, curr.getField(Fields.Field.id));
            }
        }
    }

    /**
     * Pulls JIRA data and turns it into a list of epics
     * @return A list of epics
     */
    public static HashMap<String, IssueOnline> getEpics() {
        System.out.println("Reading epics from JIRA.");
        try {
            JSONObject epics = new JSONObject(MainJira.getDataFromURL(MainJira.kEpicLink));
            HashMap<String, IssueOnline> epicList = parseEpics(epics);
            for (IssueOnline curr : epicList.values()) {
                System.out.println(curr);
            }
            return epicList;
        } catch (JSONException ex) {
            System.err.println("JSONException caught: ");
            ex.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public static HashMap<String, IssueOnline> parseEpics(JSONObject epics) {
        HashMap<String, IssueOnline> epicList = new HashMap<>();

        try {
            JSONArray jsonIssues = epics.getJSONArray(MainJira.kIssues);

            for (int i = 0; i < jsonIssues.length(); i++) {
                JSONObject curr = jsonIssues.getJSONObject(i);

                //String status, String summary, String priority, String assignee, String reporter, ArrayList<String> watchers,
                //String creationDate, String sprint, String url, String id, ArrayList<CommentJira> comments

                JSONObject fields = curr.getJSONObject("fields");
                String id = IssueJira.getID(fields);
                HashMap<Field, String> fieldsMap = new HashMap<>();

                fieldsMap.put(Fields.Field.status, JsonParser.safeSubChild(fields, "status", "statusCategory", "name"));
                fieldsMap.put(Fields.Field.summary, JsonParser.safeGet(fields, "summary"));
                fieldsMap.put(Fields.Field.priority, JsonParser.safeChild(fields, "priority", "name"));
                fieldsMap.put(Fields.Field.assignee, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.reporter, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.creationDate, Fields.humanReadable(JsonParser.safeGet(fields, "created")));
                fieldsMap.put(Fields.Field.sprint, JsonParser.safeGet(fields,"customfield_10115")); //Not quite correct
                fieldsMap.put(Fields.Field.url, "flowplay.atlassian.net/browse/" + id);
                fieldsMap.put(Fields.Field.lastUpdated, Fields.humanReadable(JsonParser.safeGet(fields,"updated")));
                fieldsMap.put(Fields.Field.severity, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.hardware, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.os, JsonParser.safeGet(fields,"customfield_10206"));
                fieldsMap.put(Fields.Field.patchIntoPlaytest, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.html5Bug, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.hasBeenPatched, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.fixBeforeHtml5, JsonParser.safeGet(fields,"customfield_10607"));
                fieldsMap.put(Fields.Field.product, JsonParser.safeGet(fields,"customfield_10700"));
                fieldsMap.put(Fields.Field.epicLink, IssueOnline.kNA);
                fieldsMap.put(Fields.Field.world, JsonParser.safeGet(fields,"customfield_10602"));
                fieldsMap.put(Fields.Field.days, IssueOnline.kNA);
                fieldsMap.put(Field.issueType, JsonParser.safeChild(fields, "issuetype","name"));

                IssueOnline epic = new IssueOnline(fieldsMap, IssueJira.getWatchers(id), IssueJira.getComments(curr, id, fieldsMap.get(Field.issueType)));

                epicList.put(id, epic);
            }

            return epicList;
        } catch (JSONException ex) {
            System.err.println("Error parsing JSON: ");
            ex.printStackTrace();
            return null;
        }
    }
}
