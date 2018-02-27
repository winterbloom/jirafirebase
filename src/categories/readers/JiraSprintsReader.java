package categories.readers;

import general.DatabaseOnline;
import individuals.SprintOnline;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains the code for reading sprint data
 *
 * User: Hazel
 * Date: 8/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraSprintsReader {

    private static HashMap<String, SprintOnline> sprints = new HashMap<>();

    public static void readSprints() {
        DatabaseOnline.readSprints(sprints);
    }

    public static HashMap<String, SprintOnline> getSprints() { return sprints; }

    public static void printSprints() {
        for (SprintOnline curr : sprints.values()) {
            System.out.println(curr);
        }
    }

}
