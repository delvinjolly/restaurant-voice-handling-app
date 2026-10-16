package com.restaurant.restaurant_app.session;

import com.restaurant.restaurant_app.entity.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderSessionManager {

    private final Map<String, Order> activeOrders = new HashMap<>();

    public void setCurrentOrder(String customerName, Order order) {

        String key = getKey(customerName);

        activeOrders.put(key, order);
    }

    public Order getCurrentOrder(String customerName) {

        String key = getKey(customerName);

        return activeOrders.get(key);
    }

    public void removeCurrentOrder(String customerName) {

        String key = getKey(customerName);

        activeOrders.remove(key);
    }

    private String getKey(String customerName) {

        if (customerName == null ||
                customerName.trim().isEmpty()) {

            return "UNKNOWN";
        }

        return customerName.trim().toLowerCase();
    }
}