package com.example.demo.dto;

import com.example.demo.entity.OrderStatus;

public record TrackingResponse(Long orderId, String trackingId, OrderStatus status, String courier) {
}
