package general;

import org.json.JSONException;
import org.json.JSONObject;

/*********************************************************
 * Description:
 *
 * Contains some helper functions for parsing json
 *
 * User: Hazel
 * Date: 8/9/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class JsonParser {

    public static final String kLs = System.lineSeparator();

    public static String safeGet(JSONObject m_fieldsJson, String field) {
        try {
            if (!m_fieldsJson.has(field)) {
                return "<= missing " + field + " => from " + m_fieldsJson;
            }
            if (!m_fieldsJson.isNull(field)) {
                String value = m_fieldsJson.getString(field);
                if (value == null) { return ""; }
                return value;
            }
            return "";
        } catch (JSONException e) {
            return "<= error " + field + " =>" ;
        }
    }

    public static String safeChild(JSONObject m_fieldsJson, String field, String childField) {
        try {
            if (!m_fieldsJson.has(field)) {
                return "<= missing " + field + " => from " + m_fieldsJson;
            }
            if (!m_fieldsJson.isNull(field)) {
                JSONObject child = m_fieldsJson.getJSONObject(field);

                if (!child.has(childField)) {
                    return "<= missing " + childField + " => from " + child;
                }

                String value = child.getString(childField);
                if (value == null) { return ""; }
                return value;
            }
            return ""; //If anything was null
        } catch (JSONException e) {
            return "<= error " + field + " =>" ;
        }
    }

    public static String safeSubChild(JSONObject m_fieldsJson, String field, String childField, String subChildField) {
        try {
            if (!m_fieldsJson.has(field)) {
                return "<= missing " + field + " => from " + m_fieldsJson;
            }
            if (!m_fieldsJson.isNull(field)) {
                JSONObject child = m_fieldsJson.getJSONObject(field);

                if (!child.has(childField)) {
                    return "<= missing " + childField + " => from " + child;
                }

                if (!child.isNull(childField)) {
                    JSONObject subChild = child.getJSONObject(childField);

                    String value = subChild.getString(subChildField);
                    if (value == null) {
                        return "";
                    }
                    return value;
                }
            }
            return ""; //If anything was null
        } catch (JSONException e) {
            return "<= error " + field + " =>" ;
        }
    }

    public static String safeSubSubChild(JSONObject m_fieldsJson, String field, String childField,
                                  String subChildField, String subSubChildField) {
        try {
            if (!m_fieldsJson.has(field)) {
                return "<= missing " + field + " => from " + m_fieldsJson;
            }
            if (!m_fieldsJson.isNull(field)) {
                JSONObject child = m_fieldsJson.getJSONObject(field);

                if (!child.has(childField)) {
                    return "<= missing " + childField + " => from " + child;
                }
                if (!child.isNull(childField)) {
                    JSONObject subChild = child.getJSONObject(childField);

                    if (!subChild.has(subChildField)) {
                        return "<= missing " + subChildField + " => from " + subChild;
                    }
                    if (!subChild.isNull(subChildField)) {
                        JSONObject subSubChild = subChild.getJSONObject(subChildField);
                        String value = subSubChild.getString(subSubChildField);
                        if (value == null) {
                            return "";
                        }
                        return value;
                    }
                }
            }
            return  ""; //If anything was null
        } catch (JSONException e) {
            return "<= error " + field + " =>" ;
        }
    }

    /**
     * Takes a json string and formats it with tabs and newlines
     * @param input An unformatted string
     * @return The correctly formatted string
     */
    public static String jsonReformat(String input) {
        StringBuilder tabs = new StringBuilder("");

        char[] inputArr = input.toCharArray();

        StringBuilder result = new StringBuilder("");
        int i = 0;
        while (i < inputArr.length) {
            char curr = inputArr[i];
            if (curr == '{' || curr == '[') { //For "{"
                tabs.append("    "); //Add one more tab
                result.append(kLs).append(tabs.toString()); //New line
            } else if (curr == '}' || curr == ']') { //For "}"
                tabs.delete(tabs.length() - 4, tabs.length()); //Cut off the last four characters; ie a tab
                result.append(kLs).append(tabs.toString()); //New line
            } else if (curr == ',') { //For ","
                result.append(kLs).append(tabs.toString()); //New line
            } else {
                result.append(curr);
            }
            i++;
        }
        return result.toString();
    }
}
