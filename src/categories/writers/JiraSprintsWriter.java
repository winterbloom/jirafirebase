package categories.writers;

import general.DatabaseOnline;
import general.JsonParser;
import general.MainJira;
import individuals.SprintOnline;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains the code for pulling sprint data
 *
 * User: Hazel
 * Date: 8/10/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraSprintsWriter {

    public static void refreshFirebaseSprints(HashMap<String, SprintOnline> sprints) {
        if (sprints != null) {
            for (SprintOnline curr : sprints.values()) {
                System.out.println(curr);
                DatabaseOnline.write(curr, curr.getID());
            }
        }
    }

    /**
     * Pulls JIRA data and turns it into a list of sprints
     * @return A list of sprints
     */
    public static HashMap<String, SprintOnline> getSprints() {
        System.out.println("Reading sprints from JIRA.");
        JSONObject sprints;
        try {
            sprints = new JSONObject(MainJira.getDataFromURL(MainJira.kSprintLink));
            return parseSprints(sprints);
        } catch (JSONException ex) {
            System.err.println("JSONException caught: ");
            ex.printStackTrace();
            System.exit(1);
            return null;
        }

    }

    /**
     * Takes a JSONObject with sprint data and turns it into a list of SprintOnlines
     * @param sprints A JSONObject with mutliple sprints' worth of data
     * @return A list of the sprints, parsed
     */
    public static HashMap<String, SprintOnline> parseSprints(JSONObject sprints) {
        //Extract the sprints from sprints and get their appropriate values
        HashMap<String, SprintOnline> sprintArray = new HashMap<>();

        try {
            JSONArray jsonArray = sprints.getJSONArray(MainJira.kSprints);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject curr = jsonArray.getJSONObject(i);

                SprintOnline sprint = new SprintOnline();

                sprint.setName(JsonParser.safeGet(curr,"name"));
                String id = JsonParser.safeGet(curr, "id");
                sprint.setID(id);
                sprint.setState(JsonParser.safeGet(curr, "state"));

                sprintArray.put(id, sprint);
            }
        } catch (JSONException ex) {
            System.err.println("Error parsing sprint JSON: ");
            ex.printStackTrace();
        }

        return sprintArray;
    }
}
