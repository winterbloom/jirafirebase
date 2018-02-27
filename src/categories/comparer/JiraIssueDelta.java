package categories.comparer;

import general.Fields;
import general.MainJira;
import individuals.IssueJira;
import individuals.IssueOnline;
import general.Fields.Field;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

import static general.JsonParser.kLs;

/*********************************************************
 * Description:
 *
 * Contains the code for a single comparison of issues - equal, or what changed
 *
 * User: Hazel
 * Date: 8/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraIssueDelta {

    private boolean wasDeleted, wasAdded;

    private HashMap<Field, String> aFieldsMap;
    private HashMap<Field, String> bFieldsMap;

    private ArrayList<String> aWatchers, bWatchers;
    private ArrayList<HashMap<String, String>> aComments, bComments;
    private IssueOnline a;
    private IssueOnline b;

    public JiraIssueDelta(IssueOnline a, IssueOnline b) {
        this.a = a;
        this.b = b;

        if (this.a == null) { wasAdded = true; }
        else {
            if (this.b == null) { //Pull it one last time to get the new data
                wasDeleted = true;

                String id = this.a.getField(Fields.Field.id);

                try {
                    String line = MainJira.getDataFromURL("https://flowplay.atlassian.net/rest/api/2/issue/" + id + "?expand=renderedFields");
                    if (line != null) {
                        JSONObject newB = new JSONObject(line);

                        //Pull the new info for B one last time and map b to that new info
                        this.b = new IssueOnline(new IssueJira(newB));
                        bFieldsMap = this.b.getFieldsMap();
                        bWatchers = this.b.getWatchers();
                        bComments = this.b.getComments();
                    } else { //The issue has been literally deleted
                        this.b = this.a;
                        bFieldsMap = this.a.getFieldsMap();
                        bWatchers = this.a.getWatchers();
                        bComments = this.a.getComments();
                        bFieldsMap.replace(Field.status, "DELETED");
                    }
                } catch (JSONException ex) {
                    System.err.println("Error trying to pull issue data for issue " + id + ":");
                    ex.printStackTrace();
                }
            }

            aFieldsMap = this.a.getFieldsMap();
            aWatchers = this.a.getWatchers();
            aComments = this.a.getComments();

            bFieldsMap = this.b.getFieldsMap();
            bWatchers = this.b.getWatchers();
            bComments = this.b.getComments();
        }
    }

    public boolean isOnlyChange(Predicate<Field> test, boolean includeCommentsAndWatchers) {
        for (Field f : Field.values()) {
            // If a field has changed then return this is not
            // the only change...unless the predicate passes.
            if (!getFieldEqual(f)) {
                if (test.test(f))
                    continue;

                // The last updated field will always change - so ignore that field
                if (f == Field.lastUpdated)
                    continue;

                // The person making a change also does not matter
                if (f == Field.changedBy)
                    continue;

                //System.out.println("Delta in " + getA().getField(Field.id) + " was " + f) ;
                return false ;
            }
        }

        // Comments and watchers are fairly expensive to compare
        if (!includeCommentsAndWatchers)
            return true ;

        if (!getWatchersEqual()) {
            //System.out.println("Delta in " + getA().getField(Field.id) + " was watchers") ;
            return false;
        }
        if (!getCommentsEqual()) {
            //System.out.println("Delta in " + getA().getField(Field.id) + " was comments") ;
            return false;
        }

        //System.out.println("Delta in " + getA().getField(Field.id) + " was a match to predicate") ;

        return true ;
    }

    public boolean getFieldEqual(Field field) {
        if (!wasAdded) {
            String aField = aFieldsMap.get(field);
            String bField = bFieldsMap.get(field);

            //If it's any one of the many default values
            if (Fields.isDefault(aField, field)) {
                aField = "";
            }

            if (Fields.isDefault(bField, field)) {
                bField = "";
            }

            return aField.equals(bField);
        }
        return false;
    }

    public boolean getCommentsEqual() {
        if (aComments == null && bComments == null) { return true; }
        if (aComments == null) {
            return bComments.size() == 0;
        } else if (bComments == null) {
            return aComments.size() == 0;
        }
        if (aComments.size() != bComments.size()) { return false; }
        for (int i = 0; i < aComments.size(); i++) {
            HashMap<String, String> aComment = aComments.get(i);
            HashMap<String, String> bComment = bComments.get(i);
            if (!aComment.equals(bComment)) { return false; }
        }
        return true;
    }

    public boolean getWatchersEqual() {
        if (aWatchers.size() != bWatchers.size()) { return false; }
        for (int i = 0; i < aWatchers.size(); i++) {
            if (!aWatchers.get(i).equals(bWatchers.get(i))) {
                return false;
            }
        }
        return true;
    }

    public IssueOnline getA() { return a; }

    public IssueOnline getB() { return b; }

    public boolean wasDeleted() { return wasDeleted; }
    public boolean wasAdded() { return wasAdded; }

    public boolean areEqual() {
        return areEqualExceptComments() &&
                getCommentsEqual() ;
    }

    public boolean areEqualExceptComments() {
        return  areEqualExceptLastUpdated() &&
                getFieldEqual(Field.lastUpdated);
    }

    public boolean areEqualExceptLastUpdated() {
        //This means something has changed, but we don't know what it is
        for (Field currField : Field.values()) {
            if (!currField.equals(Field.lastUpdated) && !getFieldEqual(currField)) {
                return false;
            }
        }
        return true;
    }

    public static JiraIssueDelta diff(IssueOnline a, IssueOnline b) {
        JiraIssueDelta newDelta = new JiraIssueDelta(a, b);

        return newDelta;
    }

    @Override
    public boolean equals(Object other) {
        JiraIssueDelta o = (JiraIssueDelta) other;

        if (this.wasAdded() || o.wasAdded()) { return false; }
        //The objects are the same and their equality is too
        for (Field currField : Fields.Field.values()) {
            if (!(this.getFieldEqual(currField) == o.getFieldEqual(currField))) {
                return false;
            }
        }
        return this.getA().equals(o.getA()) &&
                this.getB().equals(o.getB()) &&
                this.getCommentsEqual() == o.getCommentsEqual();
    }

    @Override
    public String toString() {
        if (wasDeleted) { return "Resolved " + getA() + ""; }
        else if (wasAdded) { return "Created " + getB() + ""; }
        else if (areEqual()) { return "No change in {" + getA() + "}"; }
        else {
            StringBuilder toReturn = new StringBuilder("Changed " + getA() + kLs + "Changes:");

            for(Field currField : Fields.Field.values()) {
                if  (!getFieldEqual(currField)) {
                    toReturn.append(getChangeLine(Fields.fStr(currField), a.getField(currField), b.getField(currField)));
                }
            }

            if (!getCommentsEqual()) { toReturn.append(getChangeLine("Comments", listContents(a.getComments()), listContents(b.getComments()))); }

            return toReturn.toString();
        }
    }

    private String getChangeLine(String header, String data, String data2) {
        return kLs + "\t" + header + " | " + data + " -> " + data2;
    }

    private String listContents(ArrayList<?> watchers) {
        if (watchers == null) { return ""; }

        StringBuilder toReturn = new StringBuilder();

        for (Object watcher : watchers) {
            toReturn.append(watcher).append(", ");
        }
        int len = toReturn.length();
        return toReturn.substring(0, len > 2 ? len - 2 : len); //cut off the last comma space
    }

    public IssueOnline getIssue() {
        if (getA() != null)
            return getA() ;
        return getB() ;
    }

    public String getID() {
        IssueOnline issue = getIssue() ;
        if (issue != null)
            return issue.getField(Field.id) ;

        return "None" ;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
