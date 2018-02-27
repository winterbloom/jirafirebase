package general;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.Security;
import java.util.Properties;

/*********************************************************
 * Description:
 *
 * Contains code to send an email
 *
 * User: Doug
 * Date: 8/14/2017
 * Copyright FlowPlay, Inc (www.flowplay.com)
 *********************************************************/

public class SendEmail {
	private static final String kContentTypeHTML = "text/html; charset=UTF-8";
	private static final String kContentTypePlain = "text/plain; charset=UTF-8";
	private static final String kProtocolSMTP = "smtp";

	/**
	 * Sets up the properties needed to send mail
	 * @param props The properties object to set up
	 * @param useSSL Whether to use the secure sockets layer or not
	 * @param hostname The host to use (eg: smtp.gmail.com, smtp.mandrillapp.com)
	 * @param port The port to send email off of (eg: 465)
	 */
	private static void initializeMailServerProperties(Properties props, boolean useSSL, String hostname, int port) {
		java.security.Provider[] securityProviders = Security.getProviders();
		boolean foundProvider = false;
		for (java.security.Provider p : securityProviders) {
			if (p.getClass() == com.sun.net.ssl.internal.ssl.Provider.class) {
				foundProvider = true;
			}
		}
		if ( ! foundProvider) {
			Security.addProvider( new com.sun.net.ssl.internal.ssl.Provider() );
		}

		if (useSSL) {
			props.put("mail.transport.protocol", kProtocolSMTP);
			props.put("mail."+kProtocolSMTP+".host", hostname);
			props.put("mail."+kProtocolSMTP+".auth", "true");
			props.put("mail."+kProtocolSMTP+".starttls.enable", "true");
			props.put("mail."+kProtocolSMTP+".port", port);
			props.put("mail."+kProtocolSMTP+".socketFactory.port", port);
			props.put("mail."+kProtocolSMTP+".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail."+kProtocolSMTP+".socketFactory.fallback", "false");
			props.put("mail."+kProtocolSMTP+".quitwait", "false");
		} else {
			props.put("mail.transport.protocol", kProtocolSMTP);
			props.put("mail."+kProtocolSMTP+".host", hostname);
			props.put("mail."+kProtocolSMTP+".port", port);
			props.put("mail."+kProtocolSMTP+".quitwait", "false");
		}
	}

	/**
	 * Sends an email
	 * @param to The email address to send the email to
	 * @param from The email address which is sending the email
	 * @param host The host to use (eg: smtp.gmail.com, smtp.mandrillapp.com)
	 * @param subject The subject of the email
	 * @param body The body of the email
	 * @param username The username to authenticate the sending with
	 * @param password The password to authenticate the sending with
	 */
	public static void sendMail(String to, String from, String host, String subject, String body, String username, String password) {
		Properties properties = new Properties() ;

		boolean useSSL = true ;
		int port = 465 ;

		initializeMailServerProperties(properties, useSSL, host, port) ;

		final PasswordAuthentication passwordAuthentication = new PasswordAuthentication(username, password);
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return passwordAuthentication;
			}
		});

		boolean html = true ;

		try {
			MimeMessage message = new MimeMessage(session);

			message.setFrom( new InternetAddress(from) );
			message.setReplyTo( new Address[] { new InternetAddress(from), } );

			message.setHeader("List-Unsubscribe", "unsubscribe@vegasworld.com");
			message.setHeader("Precendence", "bulk");

			message.setSubject(subject, "utf-8");
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

			message.setContent(body, html ? kContentTypeHTML : kContentTypePlain);

			Transport t = session.getTransport(kProtocolSMTP);
			try {
				t.connect(username, password);
				t.sendMessage(message, message.getAllRecipients());
			} finally {
				t.close();
			}

			System.out.println("Sent message successfully....");
		} catch (Exception mex) {
			mex.printStackTrace();
		}
	}

}
