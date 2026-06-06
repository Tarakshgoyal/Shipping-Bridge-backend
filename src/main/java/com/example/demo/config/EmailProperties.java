package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email")
public class EmailProperties {

	private String provider = "smtp";
	private String from = "taraksh9a33@gmail.com";
	private String verificationBaseUrl = "http://localhost:8080/api/auth/verify";
	private Smtp smtp = new Smtp();

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getVerificationBaseUrl() {
		return verificationBaseUrl;
	}

	public void setVerificationBaseUrl(String verificationBaseUrl) {
		this.verificationBaseUrl = verificationBaseUrl;
	}

	public Smtp getSmtp() {
		return smtp;
	}

	public void setSmtp(Smtp smtp) {
		this.smtp = smtp;
	}

	public static class Smtp {

		private String host = "smtp.gmail.com";
		private int port = 587;
		private String username;
		private String password;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
