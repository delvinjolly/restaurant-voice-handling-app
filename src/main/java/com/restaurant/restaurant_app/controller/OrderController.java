package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.entity.MenuItem;
import com.restaurant.restaurant_app.entity.Order;
import com.restaurant.restaurant_app.entity.OrderItem;
import com.restaurant.restaurant_app.entity.Restaurant;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.OrderItemRepository;
import com.restaurant.restaurant_app.repository.OrderRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @PostMapping
    public Order createOrder(
            @PathVariable Long restaurantId,
            @RequestBody CreateOrderRequest request) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCustomerName(request.getCustomerName());
        order.setStatus("PENDING");

        double totalAmount = 0;

        Order savedOrder = orderRepository.save(order);

        for (OrderItemRequest itemRequest : request.getItems()) {

            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found"));

            if (!menuItem.isAvailable()) {
                throw new RuntimeException(
                        menuItem.getName() + " is not available");
            }

            if (itemRequest.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be greater than 0");
            }

            double itemTotal =
                    menuItem.getPrice() * itemRequest.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(menuItem.getPrice());

            orderItemRepository.save(orderItem);

            totalAmount += itemTotal;
        }

        savedOrder.setTotalAmount(totalAmount);

        return orderRepository.save(savedOrder);
    }
        @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    @GetMapping("/test")
public String test() {
    return "OrderController is working";
}
    @PutMapping("/{orderId}/status")
public Order updateOrderStatus(
        @PathVariable Long orderId,
        @RequestParam String status) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    String newStatus = status.toUpperCase();

if (!newStatus.equals("PENDING")
        && !newStatus.equals("CONFIRMED")
        && !newStatus.equals("PREPARING")
        && !newStatus.equals("READY")
        && !newStatus.equals("COMPLETED")) {

    throw new RuntimeException("Invalid order status");
}

order.setStatus(newStatus);

    return orderRepository.save(order);
}

    public static class CreateOrderRequest {

        private String customerName;
        private List<OrderItemRequest> items;

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public List<OrderItemRequest> getItems() {
            return items;
        }

        public void setItems(List<OrderItemRequest> items) {
            this.items = items;
        }
    }

    public static class OrderItemRequest {

        private Long menuItemId;
        private int quantity;

        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}