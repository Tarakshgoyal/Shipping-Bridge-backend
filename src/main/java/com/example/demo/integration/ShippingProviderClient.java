package com.example.demo.integration;

public interface ShippingProviderClient {

	RateQuote calculateRate(ProviderRateRequest request);

	CreatedShipment createShipment(CreateShipmentCommand command);

	TrackingInfo fetchTracking(String trackingId);
}
