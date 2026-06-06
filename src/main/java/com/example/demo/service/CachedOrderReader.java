package com.example.demo.service;

import com.example.demo.entity.ShippingOrder;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ShippingOrderRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CachedOrderReader {

	private final ShippingOrderRepository orderRepository;

	public CachedOrderReader(ShippingOrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Cacheable(cacheNames = CacheNames.ORDER_RECORDS, key = "#id")
	@Transactional(readOnly = true)
	public OrderSnapshot getSnapshot(Long id) {
		ShippingOrder order = orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order " + id + " was not found"));
		return toSnapshot(order);
	}

	public static OrderSnapshot toSnapshot(ShippingOrder order) {
		return new OrderSnapshot(
				order.getId(),
				order.getCustomerName(),
				order.getTrackingId(),
				order.getStatus(),
				order.getCourier());
	}
}
