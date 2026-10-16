package com.restaurant.restaurant_app.repository;

import com.restaurant.restaurant_app.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRestaurantIdAndBookingDateAndBookingTime(
            Long restaurantId,
            LocalDate bookingDate,
            LocalTime bookingTime);

    boolean existsByRestaurantIdAndBookingDateAndBookingTime(
            Long restaurantId,
            LocalDate bookingDate,
            LocalTime bookingTime);
}