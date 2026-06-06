package com.example.demo.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "log")
public class LoggingEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

	@Override
	public void sendVerificationEmail(String to, String username, String verificationLink) {
		log.info("Verification email for {} <{}>: {}", username, to, verificationLink);
	}
}
