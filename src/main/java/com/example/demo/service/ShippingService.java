package com.example.demo.service;

import com.example.demo.dto.ShippingCalculationRequest;
import com.example.demo.dto.ShippingCalculationResponse;
import com.example.demo.integration.ProviderRateRequest;
import com.example.demo.integration.RateQuote;
import com.example.demo.integration.ShippingProviderClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ShippingService {

	private final ShippingProviderClient shippingProviderClient;

	public ShippingService(ShippingProviderClient shippingProviderClient) {
		this.shippingProviderClient = shippingProviderClient;
	}

	@Cacheable(
			cacheNames = CacheNames.SHIPPING_RATES,
			key = "#request.pickupPincode() + ':' + #request.deliveryPincode() + ':' + #request.weight()")
	public ShippingCalculationResponse calculate(ShippingCalculationRequest request) {
		RateQuote quote = shippingProviderClient.calculateRate(new ProviderRateRequest(
				request.pickupPincode(),
				request.deliveryPincode(),
				request.weight()));
		return new ShippingCalculationResponse(quote.courier(), quote.estimatedCost(), quote.estimatedDays());
	}
}
