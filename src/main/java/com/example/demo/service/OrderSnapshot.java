package com.example.demo.service;

import com.example.demo.entity.OrderStatus;

public record OrderSnapshot(
		Long id,
		String customerName,
		String trackingId,
		OrderStatus status,
		String courier) {
}
