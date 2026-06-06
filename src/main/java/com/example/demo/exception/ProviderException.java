package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class ProviderException extends ApiException {

	public ProviderException(String message) {
		super(HttpStatus.BAD_GATEWAY, message);
	}

	public ProviderException(String message, Throwable cause) {
		super(HttpStatus.BAD_GATEWAY, message, cause);
	}
}
