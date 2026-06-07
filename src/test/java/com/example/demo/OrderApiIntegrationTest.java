package com.example.demo;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entity.UserAccount;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private EmailVerificationTokenRepository tokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createVerifiedUser() {
		if (userAccountRepository.existsByUsername("verified_user")) {
			return;
		}
		UserAccount account = new UserAccount();
		account.setUsername("verified_user");
		account.setEmail("verified@example.com");
		account.setPasswordHash(passwordEncoder.encode("password123"));
		account.setEmailVerified(true);
		userAccountRepository.save(account);
	}

	@Test
	void calculatesShippingCost() throws Exception {
		mockMvc.perform(post("/api/shipping/calculate")
						.header("Authorization", basicAuth())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pickupPincode": "110001",
								  "deliveryPincode": "560001",
								  "weight": 1.5
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courier", is("Shiprocket")))
				.andExpect(jsonPath("$.estimatedCost", is(130)))
				.andExpect(jsonPath("$.estimatedDays", is(3)));
	}

	@Test
	void createsFetchesAndRefreshesOrder() throws Exception {
		mockMvc.perform(post("/api/orders")
						.header("Authorization", basicAuth())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerName": "John Doe",
								  "phone": "9876543210",
								  "address": "Sample Address",
								  "pincode": "560001",
								  "weight": 1.5,
								  "amount": 1200
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderId", is(1)))
				.andExpect(jsonPath("$.trackingId", startsWith("SR")))
				.andExpect(jsonPath("$.status", is("CREATED")));

		mockMvc.perform(get("/api/orders/1")
						.header("Authorization", basicAuth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerName", is("John Doe")))
				.andExpect(jsonPath("$.status", is("CREATED")));

		mockMvc.perform(get("/api/orders/1/tracking")
						.header("Authorization", basicAuth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("IN_TRANSIT")));
	}

	@Test
	void rejectsInvalidPayloads() throws Exception {
		mockMvc.perform(post("/api/shipping/calculate")
						.header("Authorization", basicAuth())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pickupPincode": "11001",
								  "deliveryPincode": "560001",
								  "weight": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.pickupPincode").exists())
				.andExpect(jsonPath("$.fieldErrors.weight").exists());
	}

	@Test
	void registersAndVerifiesEmail() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "new_user",
								  "email": "new_user@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("new_user")))
				.andExpect(jsonPath("$.status", is("PENDING_EMAIL_VERIFICATION")));

		String token = tokenRepository.findAll().stream()
				.filter(candidate -> candidate.getUserAccount().getEmail().equals("new_user@example.com"))
				.findFirst()
				.orElseThrow()
				.getToken();

		mockMvc.perform(get("/api/auth/verify")
						.param("token", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("new_user@example.com")))
				.andExpect(jsonPath("$.verified", is(true)));
	}

	@Test
	void requiresAuthenticationForShippingApis() throws Exception {
		mockMvc.perform(post("/api/shipping/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pickupPincode": "110001",
								  "deliveryPincode": "560001",
								  "weight": 1.5
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void allowsCorsPreflightForFrontend() throws Exception {
		mockMvc.perform(options("/api/auth/register")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(header().string("Access-Control-Allow-Credentials", "true"));

		mockMvc.perform(options("/api/auth/register")
						.header("Origin", "https://shipping-bridge-frontend.vercel.app")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://shipping-bridge-frontend.vercel.app"))
				.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@Test
	void exposesSwaggerUiWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("SwaggerUIBundle")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/v3/docs")));
	}

	private static String basicAuth() {
		String token = Base64.getEncoder()
				.encodeToString("verified_user:password123".getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}
}
