package com.example.demo.repository;

import com.example.demo.entity.ShippingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingOrderRepository extends JpaRepository<ShippingOrder, Long> {
}
