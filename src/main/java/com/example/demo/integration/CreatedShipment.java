package com.example.demo.integration;

import com.example.demo.entity.OrderStatus;

public record CreatedShipment(String providerOrderId, String trackingId, OrderStatus status, String courier) {
}
