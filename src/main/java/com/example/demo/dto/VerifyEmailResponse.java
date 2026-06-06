package com.example.demo.dto;

public record VerifyEmailResponse(String email, boolean verified, String status) {
}
