package com.example.demo.integration;

import com.example.demo.entity.OrderStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "logistics.provider", havingValue = "mock", matchIfMissing = true)
public class MockShippingProviderClient implements ShippingProviderClient {

	private static final String COURIER = "Shiprocket";

	@Override
	public RateQuote calculateRate(ProviderRateRequest request) {
		BigDecimal base = BigDecimal.valueOf(55);
		BigDecimal weightCharge = request.weight().multiply(BigDecimal.valueOf(20));
		BigDecimal zoneCharge = request.pickupPincode().substring(0, 2).equals(request.deliveryPincode().substring(0, 2))
				? BigDecimal.valueOf(15)
				: BigDecimal.valueOf(45);
		BigDecimal cost = base.add(weightCharge).add(zoneCharge).setScale(0, RoundingMode.HALF_UP);
		int days = zoneCharge.compareTo(BigDecimal.valueOf(15)) == 0 ? 2 : 3;
		return new RateQuote(COURIER, cost, days);
	}

	@Override
	public CreatedShipment createShipment(CreateShipmentCommand command) {
		String trackingId = "SR" + String.format("%09d", command.localOrderId());
		return new CreatedShipment("MOCK-" + command.localOrderId(), trackingId, OrderStatus.CREATED, COURIER);
	}

	@Override
	public TrackingInfo fetchTracking(String trackingId) {
		return new TrackingInfo(trackingId, OrderStatus.IN_TRANSIT, COURIER);
	}
}
