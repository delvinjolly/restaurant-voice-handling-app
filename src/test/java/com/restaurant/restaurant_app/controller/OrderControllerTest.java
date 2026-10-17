package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.entity.Order;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.OrderItemRepository;
import com.restaurant.restaurant_app.repository.OrderRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                menuItemRepository
        );
    }

    @Test
    void getOrderReturnsOrderForCorrectRestaurant() {
        Order order = new Order();

        when(orderRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(order));

        Order result = orderController.getOrder(1L, 10L);

        assertSame(order, result);
        verify(orderRepository).findByIdAndRestaurant_Id(10L, 1L);
    }

    @Test
    void getOrderRejectsOrderNotBelongingToRestaurant() {
        when(orderRepository.findByIdAndRestaurant_Id(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> orderController.getOrder(2L, 10L)
        );

        verify(orderRepository).findByIdAndRestaurant_Id(10L, 2L);
    }

    @Test
    void updateOrderStatusUsesRestaurantScopedLookup() {
        Order order = new Order();

        when(orderRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderController.updateOrderStatus(1L, 10L, "ready");

        assertSame(order, result);
        verify(orderRepository).findByIdAndRestaurant_Id(10L, 1L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusRejectsOrderNotBelongingToRestaurant() {
        when(orderRepository.findByIdAndRestaurant_Id(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> orderController.updateOrderStatus(2L, 10L, "READY")
        );

        verify(orderRepository).findByIdAndRestaurant_Id(10L, 2L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}