package com.restaurant.restaurant_app.service;

import com.restaurant.restaurant_app.dto.VoiceRequest;
import com.restaurant.restaurant_app.dto.VoiceResponse;
import com.restaurant.restaurant_app.entity.MenuItem;
import com.restaurant.restaurant_app.entity.Restaurant;
import com.restaurant.restaurant_app.repository.BookingRepository;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.OrderRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;
import com.restaurant.restaurant_app.session.OrderSessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.restaurant.restaurant_app.entity.Booking;
import java.time.LocalDate;
import java.time.LocalTime;


@ExtendWith(MockitoExtension.class)
class VoiceServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderSessionManager orderSessionManager;

    private VoiceService voiceService;

    @BeforeEach
    void setUp() {
        voiceService = new VoiceService(
                restaurantRepository,
                menuItemRepository,
                bookingRepository,
                orderRepository,
                orderSessionManager
        );
    }

    @Test
    void missingRestaurantIdReturnsError() {
        VoiceRequest request = createTestRequest(
                "I want chicken biriyani"
        );
        request.setRestaurantId(null);

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals("ERROR", response.getIntent());
        assertEquals(
                "Restaurant ID is required.",
                response.getMessage()
        );
    }

    @Test
    void emptyMessageReturnsError() {
        VoiceRequest request = createTestRequest("");

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals("ERROR", response.getIntent());
        assertEquals(
                "Customer message cannot be empty.",
                response.getMessage()
        );
    }

    @Test
    void unknownRestaurantReturnsError() {
        VoiceRequest request = createTestRequest(
                "I want chicken biriyani"
        );

        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.empty());

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals("ERROR", response.getIntent());
        assertEquals(
                "Restaurant not found.",
                response.getMessage()
        );
    }

    @Test
    void negativeQuantityReturnsOrderError() {
        assertQuantityError(
                "I want -2 chicken biriyani",
                "Quantity must be at least 1."
        );
    }

    @Test
    void zeroQuantityReturnsOrderError() {
        assertQuantityError(
                "I want 0 chicken biriyani",
                "Quantity must be at least 1."
        );
    }

    @Test
    void quantityFiftyOneReturnsOrderError() {
        assertQuantityError(
                "I want 51 chicken biriyani",
                "Quantity must be between 1 and 50."
        );
    }

    @Test
    void validQuantityReturnsOrderPendingConfirmation() {
        prepareOrderMocks();

        VoiceRequest request = createTestRequest(
                "I want 2 chicken biriyani"
        );

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals(
                "ORDER_PENDING_CONFIRMATION",
                response.getIntent()
        );

        verify(orderRepository).save(any(
                com.restaurant.restaurant_app.entity.Order.class
        ));
    }

    @Test
    void missingQuantityDefaultsToOne() {
        prepareOrderMocks();

        VoiceRequest request = createTestRequest(
                "I want chicken biriyani"
        );

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals(
                "ORDER_PENDING_CONFIRMATION",
                response.getIntent()
        );

        verify(orderRepository).save(any(
                com.restaurant.restaurant_app.entity.Order.class
        ));
    }
 @Test
 void successfulBookingReturnsBookingConfirmed() {
        Restaurant restaurant = new Restaurant();

        LocalDate bookingDate = LocalDate.now().plusDays(1);
        LocalTime bookingTime = LocalTime.of(19, 30);
   when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(bookingDate.toString());
    request.setBookingTime(bookingTime.toString());
    request.setNumberOfGuests(4);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());

    verify(bookingRepository).save(any(Booking.class));
}

@Test
void occupiedBookingSlotReturnsBookingError() {
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(true);

    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(bookingDate.toString());
    request.setBookingTime(bookingTime.toString());
    request.setNumberOfGuests(4);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "That time slot is already booked.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
}

@Test
void missingBookingDateReturnsBookingError() {
    VoiceRequest request = createTestRequest("book a table");
    request.setBookingTime("19:30");
    request.setNumberOfGuests(4);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "Please specify the booking date, for example: today or tomorrow.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
}

@Test
void missingGuestCountReturnsBookingError() {
    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(LocalDate.now().plusDays(1).toString());
    request.setBookingTime("19:30");

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "Please specify the number of guests, for example: 4 people.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
}


@Test
void moreThanFiftyGuestsReturnsBookingError() {
    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(LocalDate.now().plusDays(1).toString());
    request.setBookingTime("19:30");
    request.setNumberOfGuests(51);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "Number of guests cannot exceed 50.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
}

@Test
void fiftyGuestsCanBeBooked() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(bookingDate.toString());
    request.setBookingTime(bookingTime.toString());
    request.setNumberOfGuests(50);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
}

@Test
void bookingForYesterdayReturnsBookingError() {
    VoiceRequest request = createTestRequest("book a table yesterday");
    request.setBookingTime("19:30");
    request.setNumberOfGuests(4);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "You cannot book a table for a past date. Please choose today or a future date.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
}

@Test
void bookingForTodayCanBeConfirmed() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now();
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request = createTestRequest("book a table");
    request.setBookingDate(bookingDate.toString());
    request.setBookingTime(bookingTime.toString());
    request.setNumberOfGuests(4);

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
}

    private void assertQuantityError(
            String message,
            String expectedErrorMessage
    ) {
        prepareOrderMocks();

        VoiceRequest request = createTestRequest(message);

        VoiceResponse response =
                voiceService.handleVoice(request);

        assertEquals("ORDER_ERROR", response.getIntent());
        assertEquals(
                expectedErrorMessage,
                response.getMessage()
        );

        verify(orderRepository, never()).save(any(
                com.restaurant.restaurant_app.entity.Order.class
        ));
    }

    private void prepareOrderMocks() {
       Restaurant restaurant = new Restaurant();
        MenuItem menuItem = new MenuItem();
        menuItem.setId(1L);
        menuItem.setName("Chicken Biriyani");
        menuItem.setPrice(180.0);
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);

        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.findAll())
                .thenReturn(List.of(menuItem));
    }

    private VoiceRequest createTestRequest(String message) {
        VoiceRequest request = new VoiceRequest();

        request.setRestaurantId(1L);
        request.setCustomerName("TestCustomer");
        request.setCustomerMessage(message);

        return request;
    }
}
