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
		private String pickupPostcode = "110001";

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

		public String getPickupPostcode() {
			return pickupPostcode;
		}

		public void setPickupPostcode(String pickupPostcode) {
			this.pickupPostcode = pickupPostcode;
		}
	}
}
