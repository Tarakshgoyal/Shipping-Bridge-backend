package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateOrderRequest(
		@NotBlank @Size(max = 120)
		String customerName,

		@NotBlank @Pattern(regexp = "\\d{10}", message = "phone must be a 10 digit Indian mobile number")
		String phone,

		@NotBlank @Size(max = 500)
		String address,

		@NotBlank @Pattern(regexp = "\\d{6}", message = "pincode must be a 6 digit Indian pincode")
		String pincode,

		@NotNull @DecimalMin(value = "0.01", message = "weight must be greater than zero")
		BigDecimal weight,

		@NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero")
		BigDecimal amount) {
}
