package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.CreateOrderResponse;
import com.example.demo.dto.OrderDetailsResponse;
import com.example.demo.dto.TrackingResponse;
import com.example.demo.entity.ShippingOrder;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.integration.CreateShipmentCommand;
import com.example.demo.integration.CreatedShipment;
import com.example.demo.integration.ShippingProviderClient;
import com.example.demo.integration.TrackingInfo;
import com.example.demo.repository.ShippingOrderRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private final ShippingOrderRepository orderRepository;
	private final ShippingProviderClient shippingProviderClient;
	private final CachedOrderReader cachedOrderReader;
	private final CacheManager cacheManager;

	public OrderService(
			ShippingOrderRepository orderRepository,
			ShippingProviderClient shippingProviderClient,
			CachedOrderReader cachedOrderReader,
			CacheManager cacheManager) {
		this.orderRepository = orderRepository;
		this.shippingProviderClient = shippingProviderClient;
		this.cachedOrderReader = cachedOrderReader;
		this.cacheManager = cacheManager;
	}

	@Transactional
	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		ShippingOrder order = new ShippingOrder();
		order.setCustomerName(request.customerName());
		order.setPhone(request.phone());
		order.setAddress(request.address());
		order.setPincode(request.pincode());
		order.setWeight(request.weight());
		order.setAmount(request.amount());
		order.setCourier("Shiprocket");
		order = orderRepository.save(order);

		CreatedShipment shipment = shippingProviderClient.createShipment(new CreateShipmentCommand(
				order.getId(),
				order.getCustomerName(),
				order.getPhone(),
				order.getAddress(),
				order.getPincode(),
				order.getWeight(),
				order.getAmount()));

		order.setProviderOrderId(shipment.providerOrderId());
		order.setTrackingId(shipment.trackingId());
		order.setStatus(shipment.status());
		order.setCourier(shipment.courier());
		ShippingOrder saved = orderRepository.save(order);
		cacheOrder(saved);
		return new CreateOrderResponse(saved.getId(), saved.getTrackingId(), saved.getStatus());
	}

	@Cacheable(cacheNames = CacheNames.ORDER_DETAILS, key = "#id")
	@Transactional(readOnly = true)
	public OrderDetailsResponse getOrder(Long id) {
		OrderSnapshot order = cachedOrderReader.getSnapshot(id);
		return new OrderDetailsResponse(order.id(), order.customerName(), order.trackingId(), order.status());
	}

	@Transactional
	public TrackingResponse refreshTracking(Long id) {
		OrderSnapshot snapshot = cachedOrderReader.getSnapshot(id);
		TrackingInfo trackingInfo = shippingProviderClient.fetchTracking(snapshot.trackingId());
		ShippingOrder order = findOrder(id);
		order.setStatus(trackingInfo.status());
		order.setCourier(trackingInfo.courier());
		ShippingOrder saved = orderRepository.save(order);
		cacheOrder(saved);
		TrackingResponse response = new TrackingResponse(saved.getId(), saved.getTrackingId(), saved.getStatus(), saved.getCourier());
		put(CacheNames.TRACKING, saved.getId(), response);
		return response;
	}

	private ShippingOrder findOrder(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order " + id + " was not found"));
	}

	private void cacheOrder(ShippingOrder order) {
		OrderSnapshot snapshot = CachedOrderReader.toSnapshot(order);
		put(CacheNames.ORDER_RECORDS, order.getId(), snapshot);
		put(CacheNames.ORDER_DETAILS, order.getId(),
				new OrderDetailsResponse(order.getId(), order.getCustomerName(), order.getTrackingId(), order.getStatus()));
	}

	private void put(String cacheName, Object key, Object value) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache != null) {
			cache.put(key, value);
		}
	}
}
