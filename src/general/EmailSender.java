package general;

import categories.comparer.JiraIssueDelta;
import general.Fields.Field;
import individuals.IssueOnline;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import static general.Fields.Field.changedBy;
import static general.JsonParser.kLs;

/*********************************************************
 * Description:
 *
 * Contains helper functions for sending emails
 *
 * User: Hazel
 * Date: 8/14/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class EmailSender {

    private static boolean kSendMandrill = false ;  // Not working
    private static boolean kSendGmail = true ;     // Working

    public static void main(String[] args) {
        ArrayList<JiraIssueDelta> deltas = new ArrayList<>();
        ArrayList<String> watchers = new ArrayList<>();
        watchers.add("hazel@flowplay.com");
        ArrayList<String> watchers1 = new ArrayList<>();
        watchers1.add("hazel@flowplay.com");
        ArrayList<HashMap<String, String>> comments = new ArrayList<>();
        ArrayList<HashMap<String, String>> comments1 = new ArrayList<>();
        HashMap<String, String> comment = new HashMap<>();
        comment.put("id", "15423");
        comment.put("author","doug@flowplay.com");
        comment.put("body","test comment");
        comment.put("parentBugId", "FLO-2163");
        comment.put("parentIssueType","Task");
        HashMap<String, String> comment1 = new HashMap<>();
        comment1.put("id", "15425");
        comment1.put("author","hazel@flowplay.com");
        comment1.put("body","test comment 2");
        comment1.put("parentBugId", "FLO-2163");
        comment1.put("parentIssueType","Task");
        comments1.add(comment);
        comments1.add(comment1);

        IssueOnline iss = new IssueOnline();
        iss.setWatchers(new ArrayList<>());
        iss.setComments(comments1);
        HashMap<Field, String> fieldsMap = new HashMap<>();
        fieldsMap.put(Field.status, "Started");
        fieldsMap.put(Field.sprint, "curr");
        fieldsMap.put(Field.status, "Medium");
        fieldsMap.put(Field.id, "FLO-2163");
        fieldsMap.put(Field.url, "https://flowplay.atlassian.net/browse/flo-2163");
        fieldsMap.put(Field.assignee, "hazel@flowplay.com");
        fieldsMap.put(Field.reporter, "doug@flowplay.com");
        iss.setFieldsMap(fieldsMap);

        IssueOnline iss1 = new IssueOnline();
        iss1.setWatchers(new ArrayList<>());
        iss1.setComments(comments);
        HashMap<Field, String> fieldsMap1 = new HashMap<>();
        fieldsMap1.put(Field.status, "To-Do");
        fieldsMap1.put(Field.sprint, "current");
        fieldsMap1.put(Field.status, "High");
        fieldsMap1.put(Field.id, "FLO-2163");
        fieldsMap1.put(Field.url, "https://flowplay.atlassian.net/browse/flo-2163");
        fieldsMap1.put(Field.assignee, "hazel@flowplay.com");
        fieldsMap1.put(Field.reporter, "hazel@flowplay.com");
        iss1.setFieldsMap(fieldsMap1);

        deltas.add(new JiraIssueDelta(null, iss1));

        EmailConfig config = new EmailConfig(true);

        sendEmailsFromDeltas(deltas, config);
    }

    /**
     * Sends a different email depending on the provider used
     * @param to The email address to receive the email
     * @param subject The subject of the email
     * @param body The body of the email
     */
    public static void sendEmail(String to, String subject, String body) {
        if (kSendMandrill) {
            SendEmail.sendMail(to, "noreply0@vegasworld.com", "smtp.mandrillapp.com", subject,
                    body, "noreply0@vegasworld.com", "Q9CDA56NdLr7TTHqZKm-sQ");
        }

        if (kSendGmail) {
            SendEmail.sendMail(to, "jira@vegasworld.com", "smtp.gmail.com", subject,
                    body, "jira@vegasworld.com", "Tx56Wa55");
        }
    }

    /**
     * Given a list of deltas, it sends the appropriate emails to the appropriate people
     * about what changed
     * @param deltas The list of deltas to generate emails from
     */
    static void sendEmailsFromDeltas(List<JiraIssueDelta> deltas, EmailConfig config) {
        for (JiraIssueDelta delta : deltas) {
            if (!delta.areEqual()) { //Obviously, don't send an email if nothing's changed
                boolean wasAdded = delta.wasAdded();
                boolean wasDeleted = delta.wasDeleted();

                //new issue:
                if (wasAdded) {
                    IssueOnline b = delta.getB();
                    List<String> emailAddresses = b.getEmails() ;

                    for (String emailAddress : emailAddresses) {
                        StringBuilder sb = new StringBuilder();

                        String creator = b.getField(changedBy);
                        sb.append("<a href=\"").append(b.getField(Field.url)).append("\">").append(b.getField(Field.issueType)).append(" ").
                                append(b.getField(Field.id)).append("</a> has been created by ").append("<a href=\"mailto:").append(creator).
                                append("\">").append(creator).append("</a>:<br /><br />");

                        sb.append(getCreationEmail(b, emailAddress)) ;

                        // Send one email to one person
                        sendOneEmail(config, emailAddress, sb.toString(), b, emailAddresses);
                    }
                }
                //changed or removed issue:
                else {
                    IssueOnline b = delta.getB();

                    boolean assigneeOnly = assigneeOnly(delta) ;
                    List<String> emailAddresses = b.getEmails(assigneeOnly) ;

                    for (String emailAddress : emailAddresses) {
                        StringBuilder sb = new StringBuilder();

                        //What type of email this is
                        String changer = b.getField(changedBy);
                        if (wasDeleted) {
                            sb.append("<a href=\"").append(b.getField(Field.url)).append("\">").append(b.getField(Field.issueType)).append(" ").
                                    append(b.getField(Field.id)).append("</a> has been resolved by ").append("<a href=\"mailto:").append(changer).
                                    append("\">").append(changer).append("</a>:<br /><br />");
                        } else {
                            sb.append("<a href=\"").append(b.getField(Field.url)).append("\">").append(b.getField(Field.issueType)).append(" ").
                                    append(b.getField(Field.id)).append("</a> has been changed by ").append("<a href=\"mailto:").append(changer).
                                    append("\">").append(changer).append("</a>:<br /><br />");
                        }

                        //Get the contents of the email (ie: the changes table)
                        sb.append(getChangeEmail(delta));

                        //You are receiving this mail because...
                        if (wasDeleted) {
                            sb.append(receivingBecause(b, emailAddress, "You were the", "You were"));
                        } else {
                            sb.append(receivingBecause(b, emailAddress,"You are the","You are"));
                        }

                        // Send one email to one person
                        sendOneEmail(config, emailAddress, sb.toString(), b, emailAddresses);
                    }
                }
            }
        }
    }

    private static boolean wouldBeDuplicateEmail(String emailAddress, List<String> allEmailAddresses) {
        String[] qaTeam = new String[] {
                "chance@flowplay.com", "doug@flowplay.com", "jack@flowplay.com",
                "jon@flowplay.com", "justine@flowplay.com", "lisa@flowplay.com",
                "mckinley@flowplay.com", "michael@flowplay.com", "scottc@flowplay.com",
                "steven@flowplay.com" } ;

        // If we're sending email to bugs@flowplay.com
        // then don't additionally send email to any member of that team
        // (IF anyone wants these dupes, we can remove them from the qateam list above)
        if (allEmailAddresses.contains("bugs@flowplay.com")) {
            for (String qa : qaTeam) {
                if (emailAddress.equals(qa))
                    return true ;
            }
        }

        return false ;
    }

    private static void sendOneEmail(EmailConfig emailConfig, String emailAddress, String body, IssueOnline bug, List<String> emailAddresses) {
        String subject = "[JIRA] [" + bug.getField(Field.issueType) + " " + bug.getField(Field.id) + "] " + bug.getField(Field.summary);
        String to = emailAddress;

        if (wouldBeDuplicateEmail(emailAddress, emailAddresses)) {
            writeToOutput("Email duplicate so NOT sent", to, subject, body);
            return;
        }

        // If this bug was changed by the person we are about to send email to
        // we can suppress that - if the person wishes.
        String changedBy = bug.getField(Field.changedBy) ;
        boolean sendMyChangesToMe = emailConfig.sendMyChangesToMe(to) ;

        if (changedBy.equalsIgnoreCase(to) && !sendMyChangesToMe) {
            writeToOutput("You made last change so NOT sent", to, subject, body);
            return;
        }

        // Check that email sending is not currently disabled
        if (emailConfig.isEmailEnabled()) {
            //Send the email
            sendEmail(to, subject, body);
        }

        //Mark in the log file that we sent an email
        writeToOutput("Email sent", to, subject, body);

    }

    private static boolean assigneeOnly(JiraIssueDelta delta) {
        // If change just the sprint (or a few other low priority fields) then don't email watchers or reporter - just assignee
        // This is to reduce noise, esp when bulk moving tasks at the end of a sprint to the next sprint.
        Predicate<Field> lowImportance = field -> (field == Field.sprint || field == Field.sprints || field == Field.days || field == Field.productOwner) ;

        // Did we only change the sprint field?
        if (delta.isOnlyChange(lowImportance, true)) {
            String logMsg = "Email NOT sent to reporter or watchers for change to " + delta.getID() ;
            String desc = "Email NOT sent to reporter or watchers for change " + delta + " reporter " + delta.getIssue().getField(Field.reporter) + " watchers " + delta.getIssue().getWatchers() ;
            MainJira.writeToFile(desc + " at " + new Date() + kLs + kLs, MainJira.output, true, logMsg) ;
            return true;
        }

        return false ;
    }

    /**
     * Writes an email to the output log file
     * @param to The recipient of the email
     * @param subject The subject of the email
     * @param body The body of the email
     */
    private static void writeToOutput(String description, String to, String subject, String body) {
        MainJira.writeToFile(description + " at " + new Date() + " " + kLs + "To: " + to + kLs + "Subject: " + subject + kLs +
                "Body:" + kLs + body + kLs + "--------------------------" + kLs + kLs + kLs, MainJira.output, true, description + " " + to);
    }

    /**
     * Creates an HTML email for when an issue changes
     * @param delta An object representing the changes which occurred
     * @return The HTML email
     */
    private static String getChangeEmail(JiraIssueDelta delta) {
        StringBuilder sb = new StringBuilder();

        //Time stamp,
        IssueOnline b = delta.getB();
        IssueOnline a = delta.getA();
        sb.append("<div>").append(new Date()).append("<br /><br />");

        //This means something has changed, but we don't know what it is
        if (!delta.areEqualExceptLastUpdated()) {
            sb.append(startTable());

            //Header
            sb.append("<tr><th>What</th><th>Removed</th><th>Added</th></tr>");

            //Changes
            if (!delta.getFieldEqual(Field.id)) { sb.append(diff(b.getField(Field.issueType) + " ID", a.getField(Field.id), b.getField(Field.id))); }
            if (!delta.getFieldEqual(Field.assignee)) { sb.append(emailDiff("Assignee", a.getField(Field.assignee), b.getField(Field.assignee))); }
            if (!delta.getFieldEqual(Field.reporter)) { sb.append(emailDiff("Reporter", a.getField(Field.reporter), b.getField(Field.reporter))); }

            //Each field, add it to the table if it's changed
            for (Field field : Field.values()) {
                //Not the ones which should be links/emails
                //And no one cares when it was last updated
                //And this is not the place to point out who changed the issue
                if (field != Field.assignee && field != Field.reporter && field != Field.epicLink &&
                        field != Field.lastUpdated && field != Field.id && field != changedBy) {
                    if (!delta.getFieldEqual(field)) {
                        sb.append(diff(Fields.fStr(field), a.getField(field), b.getField(field)));
                    }
                }
            }

            //Adds a list of watchers, if applicable
            if (!delta.getWatchersEqual()) {
                sb.append("<tr><td>Watchers</td>");
                ArrayList<String> aWatchers = a.getWatchers();
                ArrayList<String> bWatchers = b.getWatchers();

                //Only add watchers which are in one list and not in the other
                ArrayList<String> aNotB = getWatcherList(aWatchers, bWatchers);
                ArrayList<String> bNotA = getWatcherList(bWatchers, aWatchers);

                sb.append(getWatcherHTMLList(aNotB));
                sb.append(getWatcherHTMLList(bNotA));
                sb.append("</tr>");
            }

            //This one's separate because it's a link
            if (!delta.getFieldEqual(Field.epicLink)) { sb.append(issueLink("Epic Link", a.getField(Field.epicLink), b.getField(Field.epicLink))); }

            //End the table
            sb.append("</tbody></table>");
        } else if (delta.getCommentsEqual()) {
            //We think nothing's changed, but yet the last-updated field has changed.
            sb.append("<strong>Something's changed, but we're not sure what it is. Please visit the issue to check.</strong><br>");
        }

        //Comments
        if (!delta.getCommentsEqual()) {
            sb.append("<div>");
            //Get the lists of comments
            ArrayList<HashMap<String, String>> aComments = delta.getA().getComments();
            ArrayList<HashMap<String, String>> bComments = delta.getB().getComments();
            if (bComments != null) {
                for (HashMap<String, String> newComment : bComments) { //Find new comments
                    if (aComments == null || !aComments.contains(newComment)) { //New comment
                        //For each comment that's new, add it to the list
                        sb.append("<br>").append(getCommentHTML(newComment, true));
                    }
                }
            }
            if (aComments != null) {
                for (HashMap<String, String> oldComment : aComments) { //Find deleted comments
                    if (bComments == null || !bComments.contains(oldComment)) { //Deleted comment
                        //For each comment that's been deleted, add it to the list
                        sb.append("<br>").append(getCommentHTML(oldComment, false));
                    }
                }
            }
            sb.append("<div>");

        }
        return sb.toString();
    }

    /**
     * Given a comment, turns it into an HTML representation
     * @param newComment The comment to turn into HTML
     * @param created Whether the comment was created or deleted
     * @return The new HTML comment
     */
    private static String getCommentHTML(HashMap<String, String> newComment, boolean created) {
        StringBuilder sb = new StringBuilder();
        String creation;
        if (created) { creation = "on "; }
        else { creation = "deleted on "; }

        String original = newComment.get("body") ;

        // Potentially modify the HTML we were sent
        String body = getCommentBodyHTML(original) ;

        //An example comment would be: (* indicates a link)
        // *Comment # id* [on | deleted on] *bug id* from *author*
        // Comment body
        String parentBugLink = "<a href=\"https://flowplay.atlassian.net/browse/" + newComment.get("parentBugId") + "\">";
        sb.append("<strong>").append(parentBugLink).append("Comment # ").append(newComment.get("id")).append("</a> ").append(creation).append(parentBugLink)
                .append(newComment.get("parentIssueType")).append(" ").append(newComment.get("parentBugId")).append("</a> from <a href=\"mailto:").append(newComment.get("author")).append("\">")
                .append(newComment.get("author")).append("</a></strong>").append("<pre>").append(body).append("</pre>");
        return sb.toString();
    }

    public static void quickTest() {
        String line = "<p>If there's no head, then the avatar does not load (which is the root cause of the bugs we had on live).  But that's a problem with avatars, not a problem with this cheat.</p>, parentBugId="+
                "FLO-3117}, {parentIssueType=Bug, author=chance@flowplay.com, id=16007, body=<p>Not sure if a new bug should be opened for this avatar issue. The headless issue, at least in OW and VG, should'"+
                "ve been fixed in\n"+
                "<span class=\"jira-issue-macro resolved\" data-jira-key=\"FLO-2758\" >"+
                "<!-- replace the span with an AUI template -->"+
                "<a href=\"https://flowplay.atlassian.net/browse/FLO-2758\" class=\"jira-issue-macro-key issue-link\"  title=\"FX: Avatars are static, never invoke new avatar tech\" >"+
                "<img class=\"icon\" src=\"https://flowplay.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10303&avatarType=issuetype\" />"+
                "        FLO-2758"+
                "        </a>"+
                "<span class=\"aui-lozenge aui-lozenge-subtle aui-lozenge-success jira-macro-single-issue-export-pdf\">DONE</span>"+
                "</span>\n"+
                "        and <a href=\"http://bugzilla.flowplay.com/bugzilla/show_bug.cgi?id=19959\" class=\"external-link\" rel=\"nofollow\">19959</a>. There must still be a way to lose one's head in FX, but I don't thin"+
                "k anyone's found a repro and I've never seen it happen. Can your cheat automatically include a head, or is that a bad idea?</p>, parentBugId=FLO-3117}, {parentIssueType=Bug, author=doug@flowp"+
                "    lay.com, id=16008, body=<p>And just to be clear - this isn't an \"add this item to what you are wearing\" cheat.   This is a \"replace your entire body with exactly this list of items\".  So if y"+
                "ou ask for an avatar with no head - that's what you get.</p>" ;

        System.out.println(line) ;
        System.out.println();
        String body = EmailSender.getCommentBodyHTML(line) ;
        System.out.println();
        System.out.println(body) ;
        System.exit(1);
    }

    /**
     * Given a body string for a comment from JIRA we may want to adjust it before including it in the email.
     *
     * @param body
     * @return
     */
    private static String getCommentBodyHTML(String body) {
/* When a reference is made to a bug, JIRA turns that reference into this piece of embedded HTML - which makes a mess in the email.

 <span class="jira-issue-macro resolved" data-jira-key="FLO-2758" >
 <!-- replace the span with an AUI template -->
 <a href="https://flowplay.atlassian.net/browse/FLO-2758" class="jira-issue-macro-key issue-link"  title="FX: Avatars are static, never invoke new avatar tech" >
 <img class="icon" src="https://flowplay.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10303&avatarType=issuetype" />
 FLO-2758
 </a>
 <span class="aui-lozenge aui-lozenge-subtle aui-lozenge-success jira-macro-single-issue-export-pdf">DONE</span>
 </span>
 */
        try {
            if (body.contains("<span") && body.contains("jira-issue-macro")) {
                int textBeforeSpan = body.indexOf("<span");
                int textAfterSpan = findEndSpan(body, textBeforeSpan+1);
                int keyField = body.indexOf("data-jira-key");
                int keyFieldQuoteStart = body.indexOf("\"", keyField);
                int keyFieldQuoteEnd = body.indexOf("\"", keyFieldQuoteStart+1);

                if (textBeforeSpan != -1 && textAfterSpan != -1 && keyField != -1 && keyFieldQuoteStart != -1 && keyFieldQuoteEnd != -1) {
                    // Go back skipping any newlines before <span>
                    textBeforeSpan = skipNewlines(body, textBeforeSpan-1, -1) ;

                    // Skip any newlines after </span> added by JIRA
                    textAfterSpan = textAfterSpan + 7 ; // </span>
                    textAfterSpan = skipNewlines(body, textAfterSpan, +1) ;

                    // The text before and after this span block
                    String beforeSpan = body.substring(0, textBeforeSpan+1);
                    String afterSpan = body.substring(textAfterSpan).trim();    // Trim to remove leading spaces JIRA inserts

                    // The FLO-2758 part
                    String floKey = body.substring(keyFieldQuoteStart+1, keyFieldQuoteEnd) ;
                    String bugLink = " <a href=\"https://flowplay.atlassian.net/browse/" + floKey + "\">" + floKey + "</a> ";

                    // The new body
                    String newBody = beforeSpan + bugLink + afterSpan ;
                    return newBody ;
                }
            }
        } catch (Exception ex) {
            System.err.println("Exception processing body comment " + body + " " + ex) ;
        }

        return body ;
    }

    private static int skipNewlines(String text, int start, int delta) {
        char ch = text.charAt(start) ;

        while (ch == '\n' || ch == '\r') {
            start = start + delta;

            if (start < 0 || start >= text.length())
                return start ;

            ch = text.charAt(start) ;
        }

        return start ;
    }

    private static String removeTrailingNewlines(String text) {
        while (text.charAt(text.length()-1) == '\n' || text.charAt(text.length()-1) == '\r')
            text = text.substring(0, text.length()-1) ;

        return text ;
    }

    // We may have nested <span> tags - so skip over any nested ones
    // and find the matching end tag
    private static int findEndSpan(String body, int start) {
        int depth = 1 ;
        int maxTries = 10 ; // Make sure we don't get stuck in an infinite loop if some malformed HTML passed in

        while (maxTries > 0) {
            maxTries-- ;
            int nextSpanStart = body.indexOf("<span", start);
            int nextSpanEnd = body.indexOf("</span>", start);

            // We have another nested <span> tag
            if (nextSpanStart < nextSpanEnd && nextSpanStart != -1) {
                depth++;
                start = nextSpanStart+1 ;
            } else {
                // end is before next start
                depth-- ;

                if (depth == 0)
                    return nextSpanEnd ;

                start = nextSpanEnd+1 ;
            }
        }

        return -1 ;
    }

    /**
     * Creates a row of an HTML table consisting of email links
     * @param category The title of the row
     * @param before The email link for the first data box
     * @param now The email link for the second data box
     * @return The HTML row
     */
    private static String emailDiff(String category, String before, String now) {
        String categoryData = "<td>" + category + "</td>";
        String beforeData = "<td><a href=\"mailto:" + before + "\">" + before + "</a></td>";
        String nowData = "<td><a href=\"mailto:" + now + "\">" + now + "</a></td>";
        String empty = "<td></td>";
        if (before.equals("")) { return "<tr>" + categoryData + empty + nowData + "</tr>"; }
        if (now.equals("")) { return  "<tr>" + categoryData + beforeData + empty + "</tr>"; }
        return "<tr>" + categoryData + beforeData + nowData + "</tr>";
    }

    /**
     * Creates a row of an HTML table consisting of links to JIRA issues
     * @param category The title of the row
     * @param before The contents of the first box (ie: the old id of the issue)
     * @param now The contents of the second box (ie: the new id of the issue)
     * @return The HTML row
     */
    private static String issueLink(String category, String before, String now) {
        return "<tr><td>" + category +
                "</td><td><a href=\"https://flowplay.atlassian.net/browse/" + before + "\">" + before +
                "</a></td><td><a href=\"https://flowplay.atlassian.net/browse/" + now + "\">" + now + "</a></td></tr>";
    }

    /**
     * Creates a row of an HTML table consisting of plain text
     * @param category The title of the row
     * @param before The plain text for the first data box
     * @param now The plain text for the second data box
     * @return The HTML row
     */
    private static String diff(String category, String before, String now) {
        return "<tr><td>" + category + "</td><td>" + before + "</td><td>" + now + "</td></tr>";
    }

    /**
     * Creates an HTML email for creating an issue
     * @param issue The issue which was created
     * @param emailAddress The email address which this email will be emailed to
     * @return The HTML email
     */
    private static String getCreationEmail(IssueOnline issue, String emailAddress) {
        StringBuilder sb = new StringBuilder();

        //Start with the date
        sb.append("<div>").append(new Date()).append("<br /><br />").append(startTable());

        //Bug ID, with a link to the issue
        String id = issue.getField(Field.id);
        if (!Fields.isDefault(id, Field.id)) {
            sb.append(getCreationLinkRow(issue.getField(Field.issueType) + " ID", id, issue.getField(Field.url)));
        }

        //Assignee, with links to email them
        String assignee = issue.getField(Field.assignee);
        if (!Fields.isDefault(assignee, Field.assignee)) {
            sb.append(getCreationLinkRow("Assignee", assignee, "mailto:" + assignee));
        }

        //Reporter, with links to email them
        String reporter = issue.getField(Field.reporter);
        if (!Fields.isDefault(reporter, Field.reporter)) {
            sb.append(getCreationLinkRow("Reporter", reporter, "mailto:" + reporter));
        }

        //Watchers, with a link to email each of them
        sb.append("<tr><th>Watchers</th>");
        sb.append(getWatcherHTMLList(issue.getWatchers()));
        sb.append("</tr>");

        //Each field, as plain text
        for (Field field : Field.values()) {
            //Ignoring the fields which should be links
            //Also, no one cares about the URL field or the last-updated field
            if (field != Field.id && field != Field.assignee && field != Field.epicLink &&
                    field != Field.reporter && field != Field.url && field != Field.lastUpdated && field != changedBy) {
                String fieldValue = issue.getField(field);
                //Don't read "" -> "---" as a change or "All" -> null as a change
                if (!Fields.isDefault(fieldValue, field)) {
                    //Add to the table
                    sb.append(getCreationRow(Fields.fStr(field), fieldValue));
                }
            }
        }

        //The epic link - separate because it's a link, not plain text
        String epicLink = issue.getField(Field.epicLink);
        if (!Fields.isDefault(epicLink, Field.epicLink)) {
            sb.append(getCreationLinkRow("Epic Link", epicLink, "https://flowplay.atlassian.net/browse/" + epicLink));
        }

        //End table
        sb.append("</tbody></table>");

        //You are receiving this mail because...
        sb.append(receivingBecause(issue, emailAddress, "You are the new", "You are now"));
        return sb.toString();
    }

    /**
     * Creates a row of a table with only two boxes: the header and the data
     * Used for an email about creating an issue
     * @param header The header of the row
     * @param content The data of the row
     * @return An HTML row of the table
     */
    private static String getCreationRow(String header, String content) {
        return "<tr><th>" + header + "</th><td>" + content + "</td></tr>";
    }

    /**
     * Creates a row of a table with only two boxes: the header, and the data with a link to it
     * Used for an email about creating an issue, with data that should be a link
     * @param header The header of the row
     * @param content The data of the row
     * @param url The link that the data should point to
     * @return An HTML row
     */
    private static String getCreationLinkRow(String header, String content, String url) {
        return "<tr><th>" + header + "</th><td><a href=\"" + url + "\">" + content + "</a></td></tr>";
    }

    /**
     * Given a list of watchers, returns that list as HTML
     * @param watchers A list of watchers to convert to HTML
     * @return An HTML table box with the watcher list
     */
    private static String getWatcherHTMLList(ArrayList<String> watchers) {
        StringBuilder sb = new StringBuilder("<td>");
        //Writes them as mailto: links with commas between
        if (watchers.size() > 0) {
            for (String watcher : watchers) {
                sb.append("<a href=\"mailto:").append(watcher).append("\">").append(watcher).append("</a>, ");
            }

            sb = new StringBuilder(sb.substring(0, sb.length() - 2)); //cut off the extra comma
        }

        return sb.append("</td>").toString();
    }

    /**
     * Takes a list of email addresses (a) and creates a list of those email addresses,
     * excluding the email addresses in b
     * @param a A list of email addresses to include unless they are in b
     * @param b A list of email addresses to exclude
     * @return An ArrayList of the email addresses in a and not in b
     */
    private static ArrayList<String> getWatcherList(ArrayList<String> a, ArrayList<String> b) {
        ArrayList<String> toReturn = new ArrayList<>();

        for (String watcher : a) {
            if (!b.contains(watcher)) {
                toReturn.add(watcher);
            }
        }

        return toReturn;
    }

    /**
     * Creates an HTML table header
     * @return The HTML to start a table
     */
    private static String startTable() {
        //Start table
        return "<table border=\"1\" cellspacing=\"0\" cellpadding=\"8\"><tbody>";
    }

    /**
     * Constructs an HTML representation of the "you are receiving this mail because:" part of an email
     * @param issue The issue you are receiving mail about
     * @param emailAddress The person who is receiving this email
     * @param youAreThe Precedes the phrase "assignee for this bug."
     * @param youAre Precedes the phrase "watching this bug."
     * @return The final HTML
     */
    private static String receivingBecause(IssueOnline issue, String emailAddress, String youAreThe, String youAre) {
        StringBuilder sb = new StringBuilder();
        boolean isWatching = issue.getWatchers().contains(emailAddress);
        boolean isAssignee = issue.getField(Field.assignee).equals(emailAddress);
        boolean isReporter = issue.getField(Field.reporter).equals(emailAddress);
        boolean isProductOwner = issue.getField(Field.productOwner).equals(emailAddress);
        String issueType = issue.getField(Field.issueType);

        //You are receiving this mail because...
        sb.append("<br><hr />You (").append(emailAddress).append(") are receiving this mail because:<ul>");
        //New list, with bullet points for each thing which is true
        if (isAssignee) {
            sb.append("<li>").append(youAreThe).append(" assignee for this " + issueType + ".</li>");
        }
        if (isReporter) {
            sb.append("<li>").append(youAreThe).append(" reporter of this" + issueType + ".</li>");
        }
        if (isProductOwner) {
            sb.append("<li>").append(youAreThe).append(" product owner of this" + issueType + ".</li>");
        }
        if (isWatching) {
            sb.append("<li>").append(youAre).append(" watching this " + issueType + ".</li>");
        }
        //Close list and body of email
        sb.append("</ul></div>");
        return sb.toString();
    }
}
