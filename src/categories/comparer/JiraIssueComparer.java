package categories.comparer;

import general.Fields;
import individuals.IssueOnline;

import java.util.*;

/*********************************************************
 * Description:
 *
 * Contains the code for comparing two issues
 *
 * User: Hazel
 * Date: 8/11/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JiraIssueComparer {

    public static ArrayList<JiraIssueDelta> getDeltas
            (Map<String, IssueOnline> oldData, Map<String, IssueOnline> newData) {

        //Check if all the IDs in oldData are in newData
        //If not, an issue has been removed
        ArrayList<JiraIssueDelta> oldInNew = new ArrayList<>();

        if (oldData != null && newData != null) {
            for (IssueOnline aCurr : oldData.values()) {
                String aID = aCurr.getField(Fields.Field.id);
                IssueOnline bCurr = newData.get(aID);

                JiraIssueDelta delta = JiraIssueDelta.diff(aCurr, bCurr);
                if (!delta.areEqual()) {
                    oldInNew.add(delta);
                }
            }

            //Check if all the IDs in newData are in oldData
            //If not, an issue has been added
            ArrayList<JiraIssueDelta> newInOld = new ArrayList<>();

            for (IssueOnline bCurr : newData.values()) {
                String bID = bCurr.getField(Fields.Field.id);
                IssueOnline aCurr = oldData.get(bID);

                JiraIssueDelta delta = JiraIssueDelta.diff(aCurr, bCurr);
                if (!delta.areEqual()) {
                    newInOld.add(delta);
                }
            }

            //Interesting trick: sets don't allow duplicates
            Set<JiraIssueDelta> deltas = new HashSet<>();
            deltas.addAll(oldInNew);
            deltas.addAll(newInOld);
            return new ArrayList<>(deltas);
        } else {
            return oldInNew;
        }

        /*oldInNew.removeAll(newInOld);
        newInOld.addAll(oldInNew);
        return newInOld;*/
    }
}
