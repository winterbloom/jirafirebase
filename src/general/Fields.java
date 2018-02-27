package general;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains helper functions for the fields used by JIRA and Firebase
 *
 * User: Hazel
 * Date: 8/17/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class Fields {

    public static final HashMap<Field, String> defaults;
    static {
        defaults = new HashMap<>();
        defaults.put(Field.reporter, "");
        defaults.put(Field.assignee, "");
        defaults.put(Field.url, "");
        defaults.put(Field.id, "");
        defaults.put(Field.status, "New");
        defaults.put(Field.sprint, "");
        defaults.put(Field.sprints, "");
        defaults.put(Field.creationDate, "");
        defaults.put(Field.world, "");
        defaults.put(Field.epicLink, "");
        defaults.put(Field.product, "---");
        defaults.put(Field.fixBeforeHtml5, "---");
        defaults.put(Field.hasBeenPatched, "---");
        defaults.put(Field.html5Bug, "---");
        defaults.put(Field.patchIntoPlaytest, "---");
        defaults.put(Field.severity, "normal");
        defaults.put(Field.lastUpdated, "");
        defaults.put(Field.priority, "Medium");
        defaults.put(Field.summary, "");
        defaults.put(Field.hardware, "All");
        defaults.put(Field.os, "All");
        defaults.put(Field.days, "");
        defaults.put(Field.issueType, "");
        defaults.put(Field.changedBy, "");
        defaults.put(Field.description, "");
        defaults.put(Field.productOwner, "");
    }

    // This converts from the field property name to the string
    // we will show in the email.
    public static String fStr(Field field) {
        StringBuilder sb = new StringBuilder();
        String name = field.name();

        // Renamed this property - but don't want to mess with
        // the fields in the JSON (which we could also do)
        if (field == Field.fixBeforeHtml5) {
            // Note: This code will add spaces and capitalize based on camelCasing in original
            name = "fixBeforeHxMobileLaunch" ;
        }

        sb.append(Character.toUpperCase(name.charAt(0)));

        int i = 1;
        while (i < name.length()) {
            char a = name.charAt(i);
            if (Character.isUpperCase(a)) {
                sb.append(' ');
                sb.append(a);
                i++;
            } else {
                sb.append(a);
                i++;
            }
        }

        return sb.toString();
    }

    public enum Field {id, assignee, reporter, productOwner, creationDate, summary, status, priority, days, sprint, sprints, severity, url, lastUpdated,
        hardware, product, world, patchIntoPlaytest, hasBeenPatched, fixBeforeHtml5, html5Bug, os, epicLink, issueType, changedBy,
        description}

    /**
     * Checks if the given string is one of a number of defaults for its field
     * @param check The string to check if it is default
     * @param field The field the string is a part of
     * @return If the string provided is a default value for the field provided
     */
    public static boolean isDefault(String check, Field field) {
        return check == null || check.equals("") || check.equals("---") ||
                check.equals(defaults.get(field));
    }

    /**
     * Makes a date human-readable
     * @param date A date formatted like: 2017-08-14T09:54:39.235-0700
     * @return A date formatted like: 08/14/2017 09:54:39
     */
    public static String humanReadable(String date) {
        try {
            String inputFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            SimpleDateFormat input = new SimpleDateFormat(inputFormat);
            Date in = input.parse(date);
            return in.toString();

            /*String outputFormat = "EEE MMM d HH:mm:ss z yyyy";
            SimpleDateFormat output = new SimpleDateFormat(outputFormat);
            Date out = output.parse(in.toString());
            return out.toString();*/
        } catch (ParseException ex) {
            System.err.println("Error trying to parse date " + date);
            ex.printStackTrace();
            return null;
        }
    }
}
