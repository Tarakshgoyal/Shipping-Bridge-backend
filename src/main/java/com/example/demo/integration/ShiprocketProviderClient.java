package com.example.demo.integration;

import com.example.demo.config.LogisticsProperties;
import com.example.demo.entity.OrderStatus;
import com.example.demo.exception.ProviderException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "logistics.provider", havingValue = "shiprocket")
public class ShiprocketProviderClient implements ShippingProviderClient {

	private final LogisticsProperties properties;
	private final RestClient restClient;

	public ShiprocketProviderClient(LogisticsProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		this.restClient = builder.baseUrl(properties.getShiprocket().getBaseUrl().toString()).build();
	}

	@Override
	public RateQuote calculateRate(ProviderRateRequest request) {
		Map<?, ?> response = get("/courier/serviceability?pickup_postcode={pickup}&delivery_postcode={delivery}&weight={weight}&cod=0",
				request.pickupPincode(), request.deliveryPincode(), request.weight());
		Map<?, ?> data = asMap(response.get("data"));
		List<?> couriers = asList(data.get("available_courier_companies"));
		if (couriers.isEmpty()) {
			throw new ProviderException("No courier service is available for this lane");
		}
		Map<?, ?> firstCourier = asMap(couriers.get(0));
		String courier = stringValue(firstCourier.get("courier_name"), "Shiprocket");
		BigDecimal rate = decimalValue(firstCourier.get("rate"));
		Integer days = integerValue(firstCourier.get("estimated_delivery_days"), 3);
		return new RateQuote(courier, rate, days);
	}

	@Override
	public CreatedShipment createShipment(CreateShipmentCommand command) {
		Map<String, Object> payload = Map.ofEntries(
				Map.entry("order_id", "LOCAL-" + command.localOrderId()),
				Map.entry("order_date", java.time.LocalDate.now().toString()),
				Map.entry("pickup_location", "Primary"),
				Map.entry("billing_customer_name", command.customerName()),
				Map.entry("billing_last_name", "."),
				Map.entry("billing_address", command.address()),
				Map.entry("billing_city", "NA"),
				Map.entry("billing_pincode", command.pincode()),
				Map.entry("billing_state", "NA"),
				Map.entry("billing_country", "India"),
				Map.entry("billing_email", "customer@example.com"),
				Map.entry("billing_phone", command.phone()),
				Map.entry("shipping_is_billing", true),
				Map.entry("order_items", List.of(Map.of(
						"name", "E-commerce Order",
						"sku", "LOCAL-" + command.localOrderId(),
						"units", 1,
						"selling_price", command.amount()))),
				Map.entry("payment_method", "Prepaid"),
				Map.entry("sub_total", command.amount()),
				Map.entry("length", 10),
				Map.entry("breadth", 10),
				Map.entry("height", 10),
				Map.entry("weight", command.weight()));

		Map<?, ?> response = post("/orders/create/adhoc", payload);
		String providerOrderId = stringValue(response.get("order_id"), "SR-ORDER-" + command.localOrderId());
		String shipmentId = stringValue(response.get("shipment_id"), providerOrderId);
		return new CreatedShipment(providerOrderId, shipmentId, OrderStatus.CREATED, "Shiprocket");
	}

	@Override
	public TrackingInfo fetchTracking(String trackingId) {
		Map<?, ?> response = get("/courier/track/shipment/{shipmentId}", trackingId);
		String status = stringValue(response.get("current_status"), "IN_TRANSIT");
		return new TrackingInfo(trackingId, mapStatus(status), "Shiprocket");
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

	private String bearerToken() {
		String token = properties.getShiprocket().getToken();
		if (token == null || token.isBlank()) {
			throw new ProviderException("Shiprocket token is not configured");
		}
		return "Bearer " + token;
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
		return new BigDecimal(stringValue(value, "0"));
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
