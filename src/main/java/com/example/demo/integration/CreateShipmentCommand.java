package com.example.demo.integration;

import java.math.BigDecimal;

public record CreateShipmentCommand(
		Long localOrderId,
		String customerName,
		String phone,
		String address,
		String pincode,
		BigDecimal weight,
		BigDecimal amount) {
}
