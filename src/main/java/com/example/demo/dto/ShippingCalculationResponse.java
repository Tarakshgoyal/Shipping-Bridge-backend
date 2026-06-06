package com.example.demo.dto;

import java.math.BigDecimal;

public record ShippingCalculationResponse(String courier, BigDecimal estimatedCost, Integer estimatedDays) {
}
