package betullam.goobi.genios;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail {

	public boolean send(String host, String to, String from, String replyTo, String subject, String message) {
		boolean mailSent = false;

		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		Session session = Session.getInstance(props, null);
		
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from));
			InternetAddress[] replyToAddress = InternetAddress.parse(replyTo);
			msg.setReplyTo(replyToAddress);
			InternetAddress[] addresses = InternetAddress.parse(to);
			msg.setRecipients(Message.RecipientType.TO, addresses);
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			msg.setText(message, "UTF-8");
			Transport.send(msg);
			
			mailSent = true;
		} catch (MessagingException e) {
			mailSent = false;
			e.printStackTrace();
		}
		
		return mailSent;
	}
}
