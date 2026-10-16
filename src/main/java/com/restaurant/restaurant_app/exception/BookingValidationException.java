package com.restaurant.restaurant_app.exception;

public class BookingValidationException extends RuntimeException {

    public BookingValidationException(String message) {
        super(message);
    }
}