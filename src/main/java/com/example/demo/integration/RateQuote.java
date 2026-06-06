package com.example.demo.integration;

import java.math.BigDecimal;

public record RateQuote(String courier, BigDecimal estimatedCost, Integer estimatedDays) {
}
