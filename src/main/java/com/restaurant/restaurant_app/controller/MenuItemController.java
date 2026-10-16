package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.entity.MenuItem;
import com.restaurant.restaurant_app.entity.Restaurant;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu")
public class MenuItemController {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemController(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository) {

        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @GetMapping
    public List<MenuItem> getMenu(
            @PathVariable Long restaurantId) {

        return menuItemRepository.findByRestaurantId(restaurantId);
    }

    @PostMapping
    public MenuItem addMenuItem(
            @PathVariable Long restaurantId,
            @RequestBody MenuItem menuItem) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RuntimeException("Restaurant not found"));

        menuItem.setRestaurant(restaurant);

        return menuItemRepository.save(menuItem);
    }

    @PutMapping("/{menuItemId}/availability")
    public MenuItem updateAvailability(
            @PathVariable Long menuItemId,
            @RequestParam boolean available) {

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException("Menu item not found"));

        menuItem.setAvailable(available);

        return menuItemRepository.save(menuItem);
    }
}