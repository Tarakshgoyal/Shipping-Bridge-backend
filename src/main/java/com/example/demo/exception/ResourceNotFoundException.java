package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
