package com.example.demo.integration.email;

import com.example.demo.config.EmailProperties;
import com.example.demo.exception.ProviderException;
import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

	private final EmailProperties properties;

	public SmtpEmailSender(EmailProperties properties) {
		this.properties = properties;
	}

	@Override
	public void sendVerificationEmail(String to, String username, String verificationLink) {
		EmailProperties.Smtp smtp = properties.getSmtp();
		if (smtp.getUsername() == null || smtp.getUsername().isBlank()) {
			throw new ProviderException("SMTP username is not configured");
		}
		if (smtp.getPassword() == null || smtp.getPassword().isBlank()) {
			throw new ProviderException("SMTP password is not configured");
		}

		JavaMailSenderImpl mailSender = mailSender(smtp);
		try {
			mailSender.send(message -> {
				MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
				helper.setFrom(properties.getFrom());
				helper.setTo(to);
				helper.setSubject("Verify your Shipping Bridge account");
				helper.setText(verificationHtml(username, verificationLink), true);
			});
		} catch (MailException ex) {
			throw new ProviderException("SMTP email request failed: " + ex.getMessage(), ex);
		}
	}

	private static JavaMailSenderImpl mailSender(EmailProperties.Smtp smtp) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(smtp.getHost());
		sender.setPort(smtp.getPort());
		sender.setUsername(smtp.getUsername());
		sender.setPassword(smtp.getPassword());

		Properties javaMailProperties = sender.getJavaMailProperties();
		javaMailProperties.put("mail.smtp.auth", "true");
		javaMailProperties.put("mail.smtp.starttls.enable", "true");
		javaMailProperties.put("mail.smtp.starttls.required", "true");
		javaMailProperties.put("mail.smtp.ssl.trust", smtp.getHost());
		javaMailProperties.put("mail.smtp.connectiontimeout", "10000");
		javaMailProperties.put("mail.smtp.timeout", "10000");
		javaMailProperties.put("mail.smtp.writetimeout", "10000");
		return sender;
	}

	private static String verificationHtml(String username, String verificationLink) {
		return """
				<p>Hi %s,</p>
				<p>Please verify your Shipping Bridge account using the link below:</p>
				<p><a href="%s">Verify email</a></p>
				<p>If you did not create this account, you can ignore this email.</p>
				""".formatted(username, verificationLink);
	}
}
