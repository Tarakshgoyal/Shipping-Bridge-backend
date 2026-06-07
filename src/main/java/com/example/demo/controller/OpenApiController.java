package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {

	@GetMapping("/v3/docs")
	public Map<String, Object> openApi() {
		return Map.of(
				"openapi", "3.0.3",
				"info", Map.of(
						"title", "Shipping Bridge Service",
						"version", "1.0.0",
						"description", "Simplified shipping APIs for an e-commerce platform"),
				"paths", Map.of(
						"/api/shipping/calculate", Map.of("post", operation(
								"Calculate shipping cost",
								"Returns provider courier, estimated cost, and delivery days.",
								"#/components/schemas/ShippingCalculationRequest",
								"#/components/schemas/ShippingCalculationResponse")),
						"/api/orders", Map.of("post", operation(
								"Create order",
								"Persists a local order and creates a provider shipment.",
								"#/components/schemas/CreateOrderRequest",
								"#/components/schemas/CreateOrderResponse")),
						"/api/orders/{id}", Map.of("get", Map.of(
								"summary", "Fetch order details",
								"parameters", List.of(pathId()),
								"responses", okResponse("#/components/schemas/OrderDetailsResponse"))),
						"/api/orders/{id}/tracking", Map.of("get", Map.of(
								"summary", "Refresh tracking status",
								"parameters", List.of(pathId()),
								"responses", okResponse("#/components/schemas/TrackingResponse"))),
						"/api/auth/register", Map.of("post", operation(
								"Register user",
								"Creates an unverified user and attempts to send a Gmail SMTP verification email.",
								"#/components/schemas/RegisterRequest",
								"#/components/schemas/RegisterResponse")),
						"/api/auth/verify", Map.of("get", Map.of(
								"summary", "Verify user email",
								"parameters", List.of(Map.of(
										"name", "token",
										"in", "query",
										"required", true,
										"schema", Map.of("type", "string"))),
								"responses", okResponse("#/components/schemas/VerifyEmailResponse")))),
				"components", Map.of("schemas", schemas()));
	}

	@GetMapping(value = {"/swagger-ui", "/swagger-ui.html"}, produces = MediaType.TEXT_HTML_VALUE)
	public String swaggerUi() {
		return """
				<!doctype html>
				<html lang="en">
				<head>
				  <meta charset="utf-8" />
				  <meta name="viewport" content="width=device-width, initial-scale=1" />
				  <title>Shipping Bridge Swagger UI</title>
				  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
				  <style>
				    body { margin: 0; background: #11131b; }
				    .swagger-ui .topbar { display: none; }
				  </style>
				</head>
				<body>
				  <div id="swagger-ui"></div>
				  <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
				  <script>
				    window.onload = () => {
				      window.ui = SwaggerUIBundle({
				        url: '/v3/docs',
				        dom_id: '#swagger-ui',
				        deepLinking: true,
				        persistAuthorization: true,
				        presets: [SwaggerUIBundle.presets.apis],
				      });
				    };
				  </script>
				</body>
				</html>
				""";
	}

	private static Map<String, Object> operation(String summary, String description, String requestRef, String responseRef) {
		return Map.of(
				"summary", summary,
				"description", description,
				"requestBody", Map.of(
						"required", true,
						"content", jsonSchema(requestRef)),
				"responses", okResponse(responseRef));
	}

	private static Map<String, Object> okResponse(String responseRef) {
		return Map.of(
				"200", Map.of("description", "Success", "content", jsonSchema(responseRef)),
				"400", Map.of("description", "Validation error"),
				"404", Map.of("description", "Resource not found"),
				"502", Map.of("description", "Logistics provider error"));
	}

	private static Map<String, Object> jsonSchema(String ref) {
		return Map.of("application/json", Map.of("schema", Map.of("$ref", ref)));
	}

	private static Map<String, Object> pathId() {
		return Map.of(
				"name", "id",
				"in", "path",
				"required", true,
				"schema", Map.of("type", "integer", "format", "int64"));
	}

	private static Map<String, Object> schemas() {
		return Map.of(
				"ShippingCalculationRequest", object(Map.of(
						"pickupPincode", stringExample("110001"),
						"deliveryPincode", stringExample("560001"),
						"weight", numberExample(1.5))),
				"ShippingCalculationResponse", object(Map.of(
						"courier", stringExample("Shiprocket"),
						"estimatedCost", numberExample(85),
						"estimatedDays", numberExample(3))),
				"CreateOrderRequest", object(Map.of(
						"customerName", stringExample("John Doe"),
						"phone", stringExample("9876543210"),
						"address", stringExample("Sample Address"),
						"pincode", stringExample("560001"),
						"weight", numberExample(1.5),
						"amount", numberExample(1200))),
				"CreateOrderResponse", object(Map.of(
						"orderId", numberExample(1),
						"trackingId", stringExample("SR123456789"),
						"status", stringExample("CREATED"))),
				"OrderDetailsResponse", object(Map.of(
						"orderId", numberExample(1),
						"customerName", stringExample("John Doe"),
						"trackingId", stringExample("SR123456789"),
						"status", stringExample("CREATED"))),
				"TrackingResponse", object(Map.of(
						"orderId", numberExample(1),
						"trackingId", stringExample("SR123456789"),
						"status", stringExample("IN_TRANSIT"),
						"courier", stringExample("Shiprocket"))),
				"RegisterRequest", object(Map.of(
						"username", stringExample("john_doe"),
						"email", stringExample("john@example.com"),
						"password", stringExample("password123"))),
				"RegisterResponse", object(Map.of(
						"userId", numberExample(1),
						"username", stringExample("john_doe"),
						"email", stringExample("john@example.com"),
						"status", stringExample("PENDING_EMAIL_VERIFICATION"),
						"verificationLink", stringExample("https://shipping-bridge-backend.onrender.com/api/auth/verify?token=abc123"),
						"emailDeliveryStatus", stringExample("SENT"))),
				"VerifyEmailResponse", object(Map.of(
						"email", stringExample("john@example.com"),
						"verified", Map.of("type", "boolean", "example", true),
						"status", stringExample("VERIFIED"))));
	}

	private static Map<String, Object> object(Map<String, Object> properties) {
		return Map.of("type", "object", "properties", properties);
	}

	private static Map<String, Object> stringExample(String example) {
		return Map.of("type", "string", "example", example);
	}

	private static Map<String, Object> numberExample(Number example) {
		return Map.of("type", "number", "example", example);
	}
}
