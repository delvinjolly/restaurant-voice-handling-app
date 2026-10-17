package com.restaurant.restaurant_app.repository;

import com.restaurant.restaurant_app.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndRestaurant_Id(Long orderId, Long restaurantId);

    Optional<Order> findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
            String customerName,
            String status
    );
}
