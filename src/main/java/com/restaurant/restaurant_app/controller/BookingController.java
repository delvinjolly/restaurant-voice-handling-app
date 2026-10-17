package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.entity.Booking;
import com.restaurant.restaurant_app.entity.Restaurant;
import com.restaurant.restaurant_app.repository.BookingRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final RestaurantRepository restaurantRepository;

    public BookingController(
            BookingRepository bookingRepository,
            RestaurantRepository restaurantRepository) {

        this.bookingRepository = bookingRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @GetMapping
    public List<Booking> getAllBookings(
            @PathVariable Long restaurantId) {

       return bookingRepository.findByRestaurant_Id(restaurantId);
    }

    @PostMapping
    public Booking createBooking(
            @PathVariable Long restaurantId,
            @RequestBody Booking booking) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        booking.setRestaurant(restaurant);

List<Booking> existingBookings =
        bookingRepository.findByRestaurantIdAndBookingDateAndBookingTime(
                restaurantId,
                booking.getBookingDate(),
                booking.getBookingTime());

if (!existingBookings.isEmpty()) {
    throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "This time slot is already booked");
}

booking.setStatus("PENDING");

return bookingRepository.save(booking);
    }

@GetMapping("/{bookingId}")
public Booking getBooking(
        @PathVariable Long restaurantId,
        @PathVariable Long bookingId) {

    return bookingRepository.findByIdAndRestaurant_Id(
                    bookingId, restaurantId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
}
    @PutMapping("/{bookingId}/status")

public Booking updateBookingStatus(
        @PathVariable Long restaurantId,
        @PathVariable Long bookingId,
        @RequestParam String status) {

    Booking booking = bookingRepository.findByIdAndRestaurant_Id(
                    bookingId, restaurantId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

    if (status == null || status.isBlank()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Booking status is required");
    }

    String newStatus = status.trim().toUpperCase(java.util.Locale.ROOT);

    if (!newStatus.equals("PENDING")
            && !newStatus.equals("CONFIRMED")
            && !newStatus.equals("CANCELLED")) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid booking status");
    }

    booking.setStatus(newStatus);
    return bookingRepository.save(booking);
}
}