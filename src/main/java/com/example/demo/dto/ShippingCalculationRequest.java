package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record ShippingCalculationRequest(
		@NotBlank @Pattern(regexp = "\\d{6}", message = "pickupPincode must be a 6 digit Indian pincode")
		String pickupPincode,

		@NotBlank @Pattern(regexp = "\\d{6}", message = "deliveryPincode must be a 6 digit Indian pincode")
		String deliveryPincode,

		@NotNull @DecimalMin(value = "0.01", message = "weight must be greater than zero")
		BigDecimal weight) {
}
