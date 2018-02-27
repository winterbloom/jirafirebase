package categories.readers;

import general.DatabaseOnline;
import individuals.IssueOnline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*********************************************************
 * Description:
 *
 * Contains the code for reading issue data
 *
 * User: Hazel
 * Date: 8/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraIssuesReader {

    private static HashMap<String, IssueOnline> issues = new HashMap<>();

    public static void readIssues() {
        DatabaseOnline.readIssues(issues);
    }

    public static void printIssues() {
        for (IssueOnline curr : issues.values()) {
            System.out.println(curr);
        }
    }

    public static HashMap<String, IssueOnline> getIssues() {
        return issues;
    }
}
