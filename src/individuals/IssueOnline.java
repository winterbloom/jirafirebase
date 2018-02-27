package individuals;

import general.Fields.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static general.Fields.Field.id;

/*********************************************************
 * Description:
 *
 * Represents a set of issues in JIRA
 *
 * User: Doug
 * Date: 6/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class IssueOnline {
    public final static String kNA = "N/A";
    public static boolean kFastIssue = false ;  // If we set this to true we'll skip watchers, comments and changed by info

    private HashMap<Field, String> fieldsMap;
    private ArrayList<String> watchers = new ArrayList<>();
    private ArrayList<HashMap<String, String>> comments = new ArrayList<>();

    public IssueOnline(HashMap<Field, String> fieldsMap, ArrayList<String> watchers, ArrayList<HashMap<String, String>> comments) {
        this.fieldsMap = fieldsMap;
        this.watchers = watchers;
        this.comments = comments;
    }

    public IssueOnline(IssueJira issue) {
        this.fieldsMap = issue.getFieldsMap();

        if (!kFastIssue) {
            this.watchers = issue.getWatchers();
            this.comments = issue.getComments();

            // To determine who last changed this bug we need to
            // consider the last comment together with another API call to JIRA
            Map<String, String> lastComment = null;
            if (comments.size() > 0) {
                lastComment = comments.get(comments.size() - 1);
            }

            this.fieldsMap.put(Field.changedBy, IssueJira.getChangedBy(fieldsMap.get(id), fieldsMap.get(Field.reporter), lastComment));
        } else {
            this.fieldsMap.put(Field.changedBy, "") ;
        }
    }

    public IssueOnline() {

    }

    public void setFieldsMap(HashMap<Field, String> fieldsMap) { this.fieldsMap = fieldsMap; }
    public void setComments(ArrayList<HashMap<String, String>> comments) { this.comments = comments; }
    public void setWatchers(ArrayList<String> watchers) { this.watchers = watchers; }

    public HashMap<Field, String> getFieldsMap() { return fieldsMap; }
    public String getField(Field field) { return fieldsMap.get(field); }

    public ArrayList<String> getWatchers() { return watchers; }
    public ArrayList<HashMap<String, String>> getComments() { return comments; }

    public ArrayList<String> getEmails() {
        return getEmails(false) ;
    }

    public ArrayList<String> getEmails(boolean assigneeOnly) {
        ArrayList<String> emails = new ArrayList<>();

        emails.add(getField(Field.assignee));

        if (!assigneeOnly) {
            emails.add(getField(Field.reporter));
            emails.addAll(getWatchers());

            String owner = getField(Field.productOwner) ;

            // Make sure we have a valid email here
            if (owner.contains("@"))
                emails.add(owner) ;
        }

        HashSet<String> set = new HashSet<>(emails);
        return new ArrayList<>(set);
    }

    @Override
    public String toString() {
        //Eg: Issue FLO-1421 (doug@flowplay.com): "HX: Fix handling of locks"
        try {
            return "issue " + getField(id) + " (" + getField(Field.assignee) + "): \"" + getField(Field.summary) + "\"";
        } catch (NullPointerException ex) {
            return "";
        }
    }

    @Override
    public boolean equals(Object other) {
        IssueOnline o = (IssueOnline) other;
        for (Field currField : Field.values()) {
            if (this.getField(currField) != null && !this.getField(currField).equals(o.getField(currField))) {
                return false;
            }

            if (this.getField(currField) == null && o.getField(currField) != null) {
                return false;
            }
        }
        return true;
    }
}
