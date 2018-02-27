package individuals;

import java.util.ArrayList;
import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Represents an issue in JIRA
 *
 * User: Doug
 * Date: 6/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class IssueFirebase {
    public ArrayList<String> watchers;
    public ArrayList<HashMap<String, String>> comments;
    public HashMap<String, String> fields;

    public IssueFirebase() {  }

    public void setComments(ArrayList<HashMap<String, String>> comments) {
        this.comments = comments;
    }

    public void setFields(HashMap<String, String> fields) {
        this.fields = fields;
    }

    public void setWatchers(ArrayList<String> watchers) {
        this.watchers = watchers;
    }
}
