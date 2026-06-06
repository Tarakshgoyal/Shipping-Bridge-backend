package com.example.demo.exception;

import com.example.demo.dto.ApiError;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.put(error.getField(), error.getDefaultMessage()));
		ApiError body = new ApiError(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Request validation failed",
				fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApiException(ApiException ex) {
		if (ex.getStatus().is5xxServerError()) {
			log.warn("API exception", ex);
		}
		ApiError body = new ApiError(
				Instant.now(),
				ex.getStatus().value(),
				ex.getStatus().getReasonPhrase(),
				ex.getMessage(),
				Map.of());
		return ResponseEntity.status(ex.getStatus()).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		log.error("Unexpected API failure", ex);
		ApiError body = new ApiError(
				Instant.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"Unexpected server error",
				Map.of());
		return ResponseEntity.internalServerError().body(body);
	}
}
