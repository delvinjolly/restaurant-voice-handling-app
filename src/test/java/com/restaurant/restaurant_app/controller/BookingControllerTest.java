package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.exception.BookingNotFoundException;
import com.restaurant.restaurant_app.entity.Booking;
import com.restaurant.restaurant_app.repository.BookingRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        bookingController = new BookingController(
                bookingRepository,
                restaurantRepository
        );
    }

    @Test
    void getAllBookingsUsesRestaurantScopedQuery() {
        List<Booking> bookings = List.of(new Booking());

        when(bookingRepository.findByRestaurant_Id(1L))
                .thenReturn(bookings);

        List<Booking> result = bookingController.getAllBookings(1L);

        assertSame(bookings, result);
        verify(bookingRepository).findByRestaurant_Id(1L);
        verify(bookingRepository, never()).findAll();
    }

    @Test
    void getBookingReturnsBookingForCorrectRestaurant() {
        Booking booking = new Booking();

        when(bookingRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(booking));
        Booking result = bookingController.getBooking(1L, 10L);

        assertSame(booking, result);
        verify(bookingRepository).findByIdAndRestaurant_Id(10L, 1L);
    }

    @Test
    void getBookingRejectsBookingFromAnotherRestaurant() {
        when(bookingRepository.findByIdAndRestaurant_Id(10L, 2L))
                .thenReturn(Optional.empty());
                assertThrows(
                BookingNotFoundException.class,
                () -> bookingController.getBooking(2L, 10L)
        );

        verify(bookingRepository).findByIdAndRestaurant_Id(10L, 2L);
    }

    @Test
    void updateBookingStatusUsesRestaurantScopedLookup() {
        Booking booking = new Booking();

        when(bookingRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result =
                bookingController.updateBookingStatus(1L, 10L, " confirmed ");

        assertSame(booking, result);
        assertEquals("CONFIRMED", booking.getStatus());
        verify(bookingRepository).findByIdAndRestaurant_Id(10L, 1L);
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateBookingStatusRejectsBookingFromAnotherRestaurant() {
        when(bookingRepository.findByIdAndRestaurant_Id(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingController.updateBookingStatus(2L, 10L, "CONFIRMED")
        );

        verify(bookingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateBookingStatusRejectsBlankStatus() {
        Booking booking = new Booking();

        when(bookingRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ResponseStatusException.class,
                () -> bookingController.updateBookingStatus(1L, 10L, "  ")
        );

        verify(bookingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateBookingStatusRejectsInvalidStatus() {
        Booking booking = new Booking();

        when(bookingRepository.findByIdAndRestaurant_Id(10L, 1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ResponseStatusException.class,
                () -> bookingController.updateBookingStatus(1L, 10L, "UNKNOWN")
        );

        verify(bookingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
