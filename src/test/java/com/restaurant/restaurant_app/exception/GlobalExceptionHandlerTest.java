
package com.restaurant.restaurant_app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void restaurantNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRestaurantNotFound(
                        new RestaurantNotFoundException("Restaurant not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Restaurant Not Found", response.getBody().get("error"));
        assertEquals("Restaurant not found", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void menuItemNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMenuItemNotFound(
                        new MenuItemNotFoundException("Menu item not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Menu Item Not Found", response.getBody().get("error"));
        assertEquals("Menu item not found", response.getBody().get("message"));
    }

    @Test
    void bookingNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBookingNotFound(
                        new BookingNotFoundException("Booking not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Booking Not Found", response.getBody().get("error"));
        assertEquals("Booking not found", response.getBody().get("message"));
    }

    @Test
    void bookingValidationReturns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBookingValidation(
                        new BookingValidationException("Invalid booking"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Booking Validation Failed", response.getBody().get("error"));
        assertEquals("Invalid booking", response.getBody().get("message"));
    }

    @Test
    void unexpectedExceptionReturnsGeneric500Message() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpectedException(
                        new RuntimeException("Sensitive internal detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals(
                "An unexpected error occurred. Please try again later.",
                response.getBody().get("message"));
    }
}
