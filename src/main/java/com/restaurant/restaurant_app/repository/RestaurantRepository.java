package com.restaurant.restaurant_app.repository;

import com.restaurant.restaurant_app.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}