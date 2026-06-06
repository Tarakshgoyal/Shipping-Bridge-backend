package com.example.demo.dto;

import com.example.demo.entity.OrderStatus;

public record CreateOrderResponse(Long orderId, String trackingId, OrderStatus status) {
}
