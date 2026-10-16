package com.restaurant.restaurant_app.repository;

import com.restaurant.restaurant_app.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantId(Long restaurantId);

    Optional<MenuItem> findByRestaurantIdAndNameIgnoreCase(
            Long restaurantId,
            String name);
}