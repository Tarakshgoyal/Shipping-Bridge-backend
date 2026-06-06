package com.example.demo.dto;

import com.example.demo.entity.OrderStatus;

public record OrderDetailsResponse(Long orderId, String customerName, String trackingId, OrderStatus status) {
}
