package com.example.demo.integration;

import java.math.BigDecimal;

public record ProviderRateRequest(String pickupPincode, String deliveryPincode, BigDecimal weight) {
}
