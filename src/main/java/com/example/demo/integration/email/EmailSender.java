package com.example.demo.integration.email;

public interface EmailSender {

	void sendVerificationEmail(String to, String username, String verificationLink);
}
