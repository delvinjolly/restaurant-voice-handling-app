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
import com.restaurant.restaurant_app.entity.Order;
import com.restaurant.restaurant_app.entity.OrderItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
void invalidExtractedBookingTimeReturnsBookingError() {
    VoiceRequest request =
            createTestRequest("book a table tomorrow at 13 PM");

    request.setBookingDate(LocalDate.now().plusDays(1).toString());
    request.setNumberOfGuests(4);
    // Leave bookingTime unset so the message parser is tested.

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_ERROR", response.getIntent());
    assertEquals(
            "Please provide the booking time.",
            response.getMessage()
    );

    verify(bookingRepository, never()).save(any(Booking.class));
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
@Test
void addingUnavailableItemReturnsOrderError() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(false);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("add chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ERROR", response.getIntent());
    assertEquals(
            "Chicken Biriyani is currently unavailable.",
            response.getMessage()
    );

    verify(orderRepository, never()).save(any(Order.class));
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
@Test
void confirmingPendingOrderReturnsOrderConfirmed() {
    Order currentOrder = new Order();
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    VoiceResponse response =
            voiceService.handleVoice(
                    createTestRequest("confirm order")
            );

    assertEquals("ORDER_CONFIRMED", response.getIntent());
    assertEquals("CONFIRMED", currentOrder.getStatus());

    verify(orderRepository).save(currentOrder);
}

@Test
void confirmingWithoutPendingOrderReturnsNoActiveOrder() {
    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(null);

    when(orderRepository
            .findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
                    "TestCustomer", "PENDING"))
            .thenReturn(Optional.empty());

    VoiceResponse response =
            voiceService.handleVoice(
                    createTestRequest("confirm order")
            );

    assertEquals("NO_ACTIVE_ORDER", response.getIntent());

    verify(orderRepository, never()).save(any(Order.class));
}

@Test
void cancellingPendingOrderReturnsOrderCancelled() {
    Order currentOrder = new Order();
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    VoiceResponse response =
            voiceService.handleVoice(
                    createTestRequest("cancel order")
            );

    assertEquals("ORDER_CANCELLED", response.getIntent());
    assertEquals("CANCELLED", currentOrder.getStatus());

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).removeCurrentOrder("TestCustomer");
}

@Test
void cancellingWithoutPendingOrderReturnsNoActiveOrder() {
    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(null);

    when(orderRepository
            .findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
                    "TestCustomer", "PENDING"))
            .thenReturn(Optional.empty());

    VoiceResponse response =
            voiceService.handleVoice(
                    createTestRequest("cancel order")
            );

    assertEquals("NO_ACTIVE_ORDER", response.getIntent());

    verify(orderRepository, never()).save(any(Order.class));
    verify(orderSessionManager, never())
            .removeCurrentOrder("TestCustomer");
}

@Test
void cancellingConfirmedOrderReturnsOrderError() {
    Order currentOrder = new Order();
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("CONFIRMED");
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    VoiceResponse response =
            voiceService.handleVoice(
                    createTestRequest("cancel order")
            );

    assertEquals("ORDER_ERROR", response.getIntent());
    assertEquals("CONFIRMED", currentOrder.getStatus());

    verify(orderRepository, never()).save(any(Order.class));
    verify(orderSessionManager, never())
            .removeCurrentOrder("TestCustomer");
}

    @Test
void removingItemNotInCurrentOrderReturnsOrderError() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    MenuItem porotta = new MenuItem();
    porotta.setId(2L);
    porotta.setName("Porotta");
    porotta.setPrice(15.0);
    porotta.setAvailable(true);
    porotta.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(porotta);
    existingItem.setQuantity(2);
    existingItem.setPrice(15.0);

    currentOrder.getItems().add(existingItem);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani, porotta));

    VoiceRequest request =
            createTestRequest("remove chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ERROR", response.getIntent());
    assertEquals(
            "Chicken Biriyani is not in your current order.",
            response.getMessage()
    );

    verify(orderRepository, never()).save(any(Order.class));
}

@Test
void removingOneItemReducesQuantityAndRecalculatesTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(3);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(540.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("remove 1 chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ITEM_REMOVED", response.getIntent());
    assertEquals(2, existingItem.getQuantity());
    assertEquals(360.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void removingAllItemsLeavesEmptyOrderWithZeroTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(3);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(540.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("remove 3 chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ITEM_REMOVED", response.getIntent());
    assertTrue(currentOrder.getItems().isEmpty());
    assertEquals(0.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void removingItemFromConfirmedOrderReturnsOrderError() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("CONFIRMED");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    VoiceRequest request =
            createTestRequest("remove chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ERROR", response.getIntent());
    assertEquals(
            "Your order cannot be modified because it is already confirmed.",
            response.getMessage()
    );
    assertEquals(2, existingItem.getQuantity());
    assertEquals(360.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository, never()).save(any(Order.class));
    verify(menuItemRepository, never()).findAll();
}

@Test
void modifyingItemWithoutQuantityReturnsOrderError() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
        createTestRequest("change chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_ERROR", response.getIntent());
    assertEquals(
            "Please specify the new quantity.",
            response.getMessage()
    );
    assertEquals(2, existingItem.getQuantity());
    assertEquals(360.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository, never()).save(any(Order.class));
    verify(orderSessionManager, never()).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void modifyingItemToValidQuantityUpdatesOrderAndTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

   VoiceRequest request =
       createTestRequest("change chicken biriyani to 4");
    VoiceResponse response =
            voiceService.handleVoice(request);
assertEquals("ORDER_UPDATED", response.getIntent());
assertEquals(4, existingItem.getQuantity());
assertEquals(720.0, currentOrder.getTotalAmount(), 0.001);

verify(orderRepository).save(currentOrder);
verify(orderSessionManager).setCurrentOrder(
        "TestCustomer", currentOrder);
}

@Test
void modifyingItemToHalfDozenUpdatesOrderAndTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("change chicken biriyani to half dozen");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_UPDATED", response.getIntent());
    assertEquals(6, existingItem.getQuantity());
    assertEquals(1080.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void modifyingItemToSpokenNumberUpdatesOrderAndTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest(
                    "change chicken biriyani to three chicken biriyani");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_UPDATED", response.getIntent());
    assertEquals(3, existingItem.getQuantity());
    assertEquals(540.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void modifyingItemToCoupleKeepsQuantityAtTwo() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("change chicken biriyani to couple");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_UPDATED", response.getIntent());
    assertEquals(2, existingItem.getQuantity());
    assertEquals(360.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}
@Test
void modifyingItemToDozenUpdatesOrderAndTotal() {
    Restaurant restaurant = new Restaurant();

    MenuItem chickenBiriyani = new MenuItem();
    chickenBiriyani.setId(1L);
    chickenBiriyani.setName("Chicken Biriyani");
    chickenBiriyani.setPrice(180.0);
    chickenBiriyani.setAvailable(true);
    chickenBiriyani.setRestaurant(restaurant);

    Order currentOrder = new Order();
    currentOrder.setRestaurant(restaurant);
    currentOrder.setCustomerName("TestCustomer");
    currentOrder.setStatus("PENDING");

    OrderItem existingItem = new OrderItem();
    existingItem.setOrder(currentOrder);
    existingItem.setMenuItem(chickenBiriyani);
    existingItem.setQuantity(2);
    existingItem.setPrice(180.0);

    currentOrder.getItems().add(existingItem);
    currentOrder.setTotalAmount(360.0);

    when(orderSessionManager.getCurrentOrder("TestCustomer"))
            .thenReturn(currentOrder);

    when(menuItemRepository.findAll())
            .thenReturn(List.of(chickenBiriyani));

    VoiceRequest request =
            createTestRequest("change chicken biriyani to a dozen");

    VoiceResponse response =
            voiceService.handleVoice(request);

    assertEquals("ORDER_UPDATED", response.getIntent());
    assertEquals(12, existingItem.getQuantity());
    assertEquals(2160.0, currentOrder.getTotalAmount(), 0.001);

    verify(orderRepository).save(currentOrder);
    verify(orderSessionManager).setCurrentOrder(
            "TestCustomer", currentOrder);
}

@Test
void bookingTimeTwelveAMIsParsedAsMidnight() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.MIDNIGHT;

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request =
            createTestRequest("book a table tomorrow at 12 AM");

    request.setBookingDate(bookingDate.toString());
    request.setNumberOfGuests(4);
    // Intentionally do not set bookingTime.
    // handleVoice() must extract "12 AM" from the message.

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());

    verify(bookingRepository).existsByRestaurantIdAndBookingDateAndBookingTime(
            1L, bookingDate, LocalTime.MIDNIGHT);

    verify(bookingRepository).save(any(Booking.class));
}

@Test
void bookingWithNumberWordGuestsIsParsed() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request =
            createTestRequest("book a table for four people tomorrow at 7:30 PM");
    request.setBookingDate(bookingDate.toString());
    // Leave bookingTime and numberOfGuests unset for parsing.

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
}

@Test
void bookingWithNumericGuestCountIsParsed() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request =
            createTestRequest("book a table for 12 guests tomorrow at 7:30 PM");
    request.setBookingDate(bookingDate.toString());
    // Leave bookingTime and numberOfGuests unset for parsing.

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
    }
@Test
void bookingWithOnePersonIsParsed() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request =
            createTestRequest("book a table for one person tomorrow at 7:30 PM");
    request.setBookingDate(bookingDate.toString());

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
}

@Test
void bookingWithTwoGuestsIsParsed() {
    Restaurant restaurant = new Restaurant();
    LocalDate bookingDate = LocalDate.now().plusDays(1);
    LocalTime bookingTime = LocalTime.of(19, 30);

    when(bookingRepository
            .existsByRestaurantIdAndBookingDateAndBookingTime(
                    1L, bookingDate, bookingTime))
            .thenReturn(false);

    when(restaurantRepository.findById(1L))
            .thenReturn(Optional.of(restaurant));

    VoiceRequest request =
            createTestRequest("book a table for two guests tomorrow at 7:30 PM");
    request.setBookingDate(bookingDate.toString());

    VoiceResponse response = voiceService.handleVoice(request);

    assertEquals("BOOKING_CONFIRMED", response.getIntent());
    verify(bookingRepository).save(any(Booking.class));
}

}
