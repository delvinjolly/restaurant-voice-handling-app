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