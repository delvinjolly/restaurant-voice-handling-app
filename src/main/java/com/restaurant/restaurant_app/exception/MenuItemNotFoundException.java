package com.restaurant.restaurant_app.exception;

public class MenuItemNotFoundException extends RuntimeException {

    public MenuItemNotFoundException(String message) {
        super(message);
    }
}