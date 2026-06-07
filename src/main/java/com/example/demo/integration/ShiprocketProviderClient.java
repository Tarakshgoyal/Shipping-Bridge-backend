package com.example.demo.integration;

import com.example.demo.config.LogisticsProperties;
import com.example.demo.entity.OrderStatus;
import com.example.demo.exception.ProviderException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "logistics.provider", havingValue = "shiprocket")
public class ShiprocketProviderClient implements ShippingProviderClient {

	private static final String COURIER = "Shiprocket";
	private static final long TOKEN_REFRESH_SAFETY_SECONDS = 3600;

	private final LogisticsProperties properties;
	private final RestClient restClient;
	private String cachedToken;
	private Instant tokenExpiresAt = Instant.EPOCH;

	public ShiprocketProviderClient(LogisticsProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		this.restClient = builder.baseUrl(properties.getShiprocket().getBaseUrl().toString()).build();
	}

	@Override
	public RateQuote calculateRate(ProviderRateRequest request) {
		Map<?, ?> response = get(
				"/courier/serviceability?pickup_postcode={pickup}&delivery_postcode={delivery}&weight={weight}&cod=0",
				request.pickupPincode(), request.deliveryPincode(), request.weight());
		Map<?, ?> data = asMap(response.get("data"));
		List<?> couriers = asList(data.get("available_courier_companies"));
		if (couriers.isEmpty()) {
			throw new ProviderException("No courier service is available for this lane");
		}
		Map<?, ?> firstCourier = couriers.stream()
				.map(ShiprocketProviderClient::asMap)
				.min(Comparator.comparing(ShiprocketProviderClient::courierRate))
				.orElseThrow(() -> new ProviderException("No courier service is available for this lane"));
		String courier = stringValue(firstCourier.get("courier_name"), COURIER);
		BigDecimal rate = decimalValue(firstCourier.get("rate"));
		Integer days = integerValue(firstCourier.get("estimated_delivery_days"), 3);
		return new RateQuote(courier, rate, days);
	}

	@Override
	public CreatedShipment createShipment(CreateShipmentCommand command) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("order_id", "LOCAL-" + command.localOrderId());
		payload.put("order_date", LocalDate.now().toString());
		payload.put("pickup_location", requiredPickupLocation());
		payload.put("billing_customer_name", command.customerName());
		payload.put("billing_last_name", ".");
		payload.put("billing_address", command.address());
		payload.put("billing_city", "NA");
		payload.put("billing_pincode", command.pincode());
		payload.put("billing_state", "NA");
		payload.put("billing_country", "India");
		payload.put("billing_email", properties.getShiprocket().getCustomerEmailFallback());
		payload.put("billing_phone", command.phone());
		payload.put("shipping_is_billing", true);
		payload.put("order_items", List.of(Map.of(
				"name", "E-commerce Order",
				"sku", "LOCAL-" + command.localOrderId(),
				"units", 1,
				"selling_price", command.amount())));
		payload.put("payment_method", "Prepaid");
		payload.put("sub_total", command.amount());
		payload.put("length", 10);
		payload.put("breadth", 10);
		payload.put("height", 10);
		payload.put("weight", command.weight());

		Map<?, ?> response = post("/orders/create/adhoc", payload);
		String providerOrderId = stringValue(response.get("order_id"), "SR-ORDER-" + command.localOrderId());
		String shipmentId = stringValue(response.get("shipment_id"), providerOrderId);
		Map<?, ?> awbResponse = post("/courier/assign/awb", Map.of("shipment_id", shipmentId));
		String awbCode = findString(awbResponse, "awb_code").orElse(shipmentId);
		String courier = findString(awbResponse, "courier_name").orElse(COURIER);
		return new CreatedShipment(providerOrderId, awbCode, OrderStatus.CREATED, courier);
	}

	@Override
	public TrackingInfo fetchTracking(String trackingId) {
		Map<?, ?> response = get("/courier/track/awb/{awb}", trackingId);
		String status = findString(response, "current_status")
				.or(() -> findString(response, "shipment_status"))
				.or(() -> findString(response, "status"))
				.orElse("IN_TRANSIT");
		String courier = findString(response, "courier_name").orElse(COURIER);
		return new TrackingInfo(trackingId, mapStatus(status), courier);
	}

	private Map<?, ?> get(String uri, Object... variables) {
		try {
			return restClient.get()
					.uri(uri, variables)
					.header(HttpHeaders.AUTHORIZATION, bearerToken())
					.retrieve()
					.body(Map.class);
		} catch (RestClientException ex) {
			throw new ProviderException("Shiprocket request failed: " + ex.getMessage(), ex);
		}
	}

	private Map<?, ?> post(String uri, Object payload) {
		try {
			return restClient.post()
					.uri(uri)
					.header(HttpHeaders.AUTHORIZATION, bearerToken())
					.body(payload)
					.retrieve()
					.body(Map.class);
		} catch (RestClientException ex) {
			throw new ProviderException("Shiprocket request failed: " + ex.getMessage(), ex);
		}
	}

	private synchronized String bearerToken() {
		String token = properties.getShiprocket().getToken();
		if (token == null || token.isBlank()) {
			token = currentOrFreshToken();
		}
		return "Bearer " + token;
	}

	private String currentOrFreshToken() {
		if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
			return cachedToken;
		}
		String email = properties.getShiprocket().getEmail();
		String password = properties.getShiprocket().getPassword();
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			throw new ProviderException("Shiprocket API user email/password are not configured");
		}
		try {
			Map<?, ?> response = restClient.post()
					.uri("/auth/login")
					.body(Map.of("email", email, "password", password))
					.retrieve()
					.body(Map.class);
			cachedToken = findString(response, "token")
					.orElseThrow(() -> new ProviderException("Shiprocket auth response did not include a token"));
			tokenExpiresAt = Instant.now().plusSeconds(240 * 60 * 60L - TOKEN_REFRESH_SAFETY_SECONDS);
			return cachedToken;
		} catch (RestClientException ex) {
			throw new ProviderException("Shiprocket authentication failed: " + ex.getMessage(), ex);
		}
	}

	private String requiredPickupLocation() {
		String pickupLocation = properties.getShiprocket().getPickupLocation();
		if (pickupLocation == null || pickupLocation.isBlank()) {
			throw new ProviderException("Shiprocket pickup location is not configured");
		}
		return pickupLocation;
	}

	private static Map<?, ?> asMap(Object value) {
		return value instanceof Map<?, ?> map ? map : Map.of();
	}

	private static List<?> asList(Object value) {
		return value instanceof List<?> list ? list : List.of();
	}

	private static String stringValue(Object value, String fallback) {
		return value == null ? fallback : value.toString();
	}

	private static BigDecimal decimalValue(Object value) {
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		try {
			return new BigDecimal(stringValue(value, "0"));
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private static BigDecimal courierRate(Map<?, ?> courier) {
		BigDecimal rate = decimalValue(courier.get("rate"));
		return rate.signum() < 0 ? BigDecimal.ZERO : rate;
	}

	private static Integer integerValue(Object value, Integer fallback) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(stringValue(value, fallback.toString()));
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	private static Optional<String> findString(Object value, String key) {
		List<Object> values = new ArrayList<>();
		collectValues(value, key, values);
		return values.stream()
				.filter(candidate -> candidate != null && !candidate.toString().isBlank())
				.map(Object::toString)
				.findFirst();
	}

	private static void collectValues(Object value, String key, List<Object> values) {
		if (value instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (key.equals(entry.getKey())) {
					values.add(entry.getValue());
				}
				collectValues(entry.getValue(), key, values);
			}
			return;
		}
		if (value instanceof List<?> list) {
			for (Object item : list) {
				collectValues(item, key, values);
			}
		}
	}

	private static OrderStatus mapStatus(String providerStatus) {
		String normalized = providerStatus.toUpperCase();
		if (normalized.contains("DELIVERED")) {
			return OrderStatus.DELIVERED;
		}
		if (normalized.contains("PICK")) {
			return OrderStatus.PICKED_UP;
		}
		if (normalized.contains("CANCEL")) {
			return OrderStatus.CANCELLED;
		}
		if (normalized.contains("FAIL")) {
			return OrderStatus.FAILED;
		}
		return OrderStatus.IN_TRANSIT;
	}
}
