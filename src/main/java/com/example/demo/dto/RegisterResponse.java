package com.example.demo.dto;

public record RegisterResponse(
		Long userId,
		String username,
		String email,
		String status,
		String verificationLink,
		String emailDeliveryStatus) {
}
