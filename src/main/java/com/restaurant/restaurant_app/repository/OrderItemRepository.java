package com.restaurant.restaurant_app.repository;

import com.restaurant.restaurant_app.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}