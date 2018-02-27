package general;

import java.util.HashMap;

/*********************************************************
 * Description:
 *
 * Contains the config information for who wants to receive what JIRA emails
 *
 * User: Hazel
 * Date: 8/15/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class EmailConfig {
    private HashMap<String, Boolean> sendMyChangesToMe = new HashMap<>();
    private final boolean m_emailEnabled ;

    public EmailConfig(boolean emailEnabled) {
        m_emailEnabled = emailEnabled ;
    }

    public boolean isEmailEnabled() {
        return m_emailEnabled;
    }

    public void addSendMyChangesToMe(String email, boolean state) {
        sendMyChangesToMe.put(email, state) ;
    }

    public boolean sendMyChangesToMe(String emailAddress) {
        boolean receivingEmail = sendMyChangesToMe.getOrDefault(emailAddress, false);
        return receivingEmail;
    }
}
