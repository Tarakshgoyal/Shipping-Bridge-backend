package com.example.demo.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logistics")
public class LogisticsProperties {

	private String provider = "mock";
	private Shiprocket shiprocket = new Shiprocket();

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Shiprocket getShiprocket() {
		return shiprocket;
	}

	public void setShiprocket(Shiprocket shiprocket) {
		this.shiprocket = shiprocket;
	}

	public static class Shiprocket {

		private URI baseUrl = URI.create("https://apiv2.shiprocket.in/v1/external");
		private String token;
		private String email;
		private String password;
		private String pickupLocation = "Primary";
		private String pickupPostcode = "110001";
		private String customerEmailFallback = "customer@example.com";

		public URI getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(URI baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPickupLocation() {
			return pickupLocation;
		}

		public void setPickupLocation(String pickupLocation) {
			this.pickupLocation = pickupLocation;
		}

		public String getPickupPostcode() {
			return pickupPostcode;
		}

		public void setPickupPostcode(String pickupPostcode) {
			this.pickupPostcode = pickupPostcode;
		}

		public String getCustomerEmailFallback() {
			return customerEmailFallback;
		}

		public void setCustomerEmailFallback(String customerEmailFallback) {
			this.customerEmailFallback = customerEmailFallback;
		}
	}
}
