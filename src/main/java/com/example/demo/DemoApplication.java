package com.example.demo;

import com.example.demo.config.EmailProperties;
import com.example.demo.config.LogisticsProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.MapPropertySource;

@SpringBootApplication
@EnableConfigurationProperties({LogisticsProperties.class, EmailProperties.class})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(DemoApplication.class);
		application.addInitializers(context -> {
			Map<String, Object> renderProperties = renderDatabaseProperties(System.getenv());
			if (!renderProperties.isEmpty()) {
				context.getEnvironment().getPropertySources()
						.addFirst(new MapPropertySource("renderDatabaseProperties", renderProperties));
			}
		});
		application.run(args);
	}

	private static Map<String, Object> renderDatabaseProperties(Map<String, String> env) {
		String explicitJdbcUrl = env.get("DB_URL");
		if (explicitJdbcUrl != null && !explicitJdbcUrl.isBlank()) {
			return dialectProperties();
		}

		String databaseUrl = env.get("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return dialectProperties();
		}

		URI uri = URI.create(databaseUrl);
		String userInfo = uri.getUserInfo() == null ? ":" : uri.getUserInfo();
		String[] credentials = userInfo.split(":", 2);
		String username = decode(credentials[0]);
		String password = credentials.length > 1 ? decode(credentials[1]) : "";
		String query = normalizedQuery(uri.getQuery());
		String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
		String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + uri.getPath() + query;

		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.datasource.url", jdbcUrl);
		properties.put("spring.datasource.username", username);
		properties.put("spring.datasource.password", password);
		properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
		properties.putAll(dialectProperties());
		return properties;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String normalizedQuery(String query) {
		if (query == null || query.isBlank()) {
			return "";
		}
		return "?" + query.replace("channel_binding=", "channelBinding=");
	}

	private static Map<String, Object> dialectProperties() {
		return Map.of(
				"spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect",
				"spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
	}
}
