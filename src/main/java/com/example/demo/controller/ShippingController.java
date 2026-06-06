package com.example.demo.controller;

import com.example.demo.dto.ShippingCalculationRequest;
import com.example.demo.dto.ShippingCalculationResponse;
import com.example.demo.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

	private final ShippingService shippingService;

	public ShippingController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@PostMapping("/calculate")
	@ResponseStatus(HttpStatus.OK)
	public ShippingCalculationResponse calculate(@Valid @RequestBody ShippingCalculationRequest request) {
		return shippingService.calculate(request);
	}
}
