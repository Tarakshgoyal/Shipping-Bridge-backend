package com.example.demo.integration;

import com.example.demo.entity.OrderStatus;

public record TrackingInfo(String trackingId, OrderStatus status, String courier) {
}
