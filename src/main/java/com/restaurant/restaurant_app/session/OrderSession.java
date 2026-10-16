package com.restaurant.restaurant_app.session;

import com.restaurant.restaurant_app.entity.Order;

public class OrderSession {

    private Order currentOrder;

    public Order getCurrentOrder() {
        return currentOrder;
    }

    public void setCurrentOrder(Order currentOrder) {
        this.currentOrder = currentOrder;
    }

    public void clearOrder() {
        this.currentOrder = null;
    }
}
