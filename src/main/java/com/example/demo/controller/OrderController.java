package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.CreateOrderResponse;
import com.example.demo.dto.OrderDetailsResponse;
import com.example.demo.dto.TrackingResponse;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreateOrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
		return orderService.createOrder(request);
	}

	@GetMapping("/{id}")
	public OrderDetailsResponse getById(@PathVariable Long id) {
		return orderService.getOrder(id);
	}

	@GetMapping("/{id}/tracking")
	public TrackingResponse refreshTracking(@PathVariable Long id) {
		return orderService.refreshTracking(id);
	}
}
