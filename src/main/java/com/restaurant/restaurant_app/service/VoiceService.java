package com.restaurant.restaurant_app.service;

import com.restaurant.restaurant_app.dto.VoiceRequest;
import com.restaurant.restaurant_app.dto.VoiceResponse;
import com.restaurant.restaurant_app.entity.Booking;
import com.restaurant.restaurant_app.entity.MenuItem;
import com.restaurant.restaurant_app.entity.Order;
import com.restaurant.restaurant_app.entity.OrderItem;
import com.restaurant.restaurant_app.entity.Restaurant;
import com.restaurant.restaurant_app.repository.BookingRepository;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.OrderRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;
import com.restaurant.restaurant_app.session.OrderSessionManager;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final OrderSessionManager orderSessionManager;

    public VoiceService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            BookingRepository bookingRepository,
            OrderRepository orderRepository,
            OrderSessionManager orderSessionManager) {

        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.orderSessionManager = orderSessionManager;
    }

    public VoiceResponse handleVoice(VoiceRequest request) {

        System.out.println(
                "Voice request received: restaurantId="
                        + request.getRestaurantId()
                        + ", customerName="
                        + request.getCustomerName()
                        + ", message="
                        + request.getCustomerMessage()
        );

        // =========================================================
        // 0. VALIDATION
        // =========================================================

        if (request.getCustomerMessage() == null
                || request.getCustomerMessage().trim().isEmpty()) {

            return new VoiceResponse(
                    "ERROR",
                    "Customer message cannot be empty."
            );
        }

        if (request.getRestaurantId() == null) {

            return new VoiceResponse(
                    "ERROR",
                    "Restaurant ID is required."
            );
        }

        String text =
                request.getCustomerMessage()
                        .trim()
                        .toLowerCase();

        // =========================================================
        // 1. CONFIRM EXISTING ORDER
        // =========================================================

        if (text.equals("yes")
                || text.equals("yes please")
                || text.equals("confirm")
                || text.equals("confirm order")
                || text.equals("confirm my order")) {

            Order currentOrder =
                    orderSessionManager.getCurrentOrder(
                            request.getCustomerName()
                    );

            // If session is empty, find pending order from database
            if (currentOrder == null) {

                currentOrder =
                        orderRepository
                                .findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
                                        request.getCustomerName(),
                                        "PENDING"
                                )
                                .orElse(null);

                if (currentOrder != null) {

                    orderSessionManager.setCurrentOrder(
                            request.getCustomerName(),
                            currentOrder
                    );
                }
            }

            // No pending order
            if (currentOrder == null) {

                return new VoiceResponse(
                        "NO_ACTIVE_ORDER",
                        "There is no pending order to confirm."
                );
            }

            // Make sure order is pending
            if (!"PENDING".equalsIgnoreCase(
                    currentOrder.getStatus())) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Your current order is not waiting for confirmation."
                );
            }

            // Confirm order
            currentOrder.setStatus("CONFIRMED");

            orderRepository.save(currentOrder);

            return new VoiceResponse(
                    "ORDER_CONFIRMED",
                    "Your order has been confirmed successfully. Total: ₹"
                            + currentOrder.getTotalAmount()
                            + "."
            );
        }

        // =========================================================
        // 2. CANCEL EXISTING ORDER
        // =========================================================

        if (text.equals("cancel order")
                || text.equals("cancel my order")
                || text.equals("i want to cancel my order")
                || text.equals("please cancel my order")
                || text.equals("cancel the order")) {

            Order currentOrder =
                    orderSessionManager.getCurrentOrder(
                            request.getCustomerName()
                    );

            // If session is empty, find pending order
            if (currentOrder == null) {

                currentOrder =
                        orderRepository
                                .findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
                                        request.getCustomerName(),
                                        "PENDING"
                                )
                                .orElse(null);

                if (currentOrder != null) {

                    orderSessionManager.setCurrentOrder(
                            request.getCustomerName(),
                            currentOrder
                    );
                }
            }

            // No pending order
            if (currentOrder == null) {

                return new VoiceResponse(
                        "NO_ACTIVE_ORDER",
                        "There is no pending order to cancel."
                );
            }

            // Only pending orders can be cancelled
            if (!"PENDING".equalsIgnoreCase(
                    currentOrder.getStatus())) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Your order cannot be cancelled because it is already "
                                + currentOrder.getStatus().toLowerCase()
                                + "."
                );
            }

            // Cancel order
            currentOrder.setStatus("CANCELLED");

            orderRepository.save(currentOrder);

            // Remove from active session
            orderSessionManager.removeCurrentOrder(
                    request.getCustomerName()
            );

            return new VoiceResponse(
                    "ORDER_CANCELLED",
                    "Your order has been cancelled successfully."
            );
        }

        // =========================================================
        // 3. ADD ITEM TO EXISTING ORDER
        // =========================================================

        if (text.contains("add")
                || text.contains("also want")
                || text.contains("also get")
                || text.contains("also give")) {

            Order currentOrder =
                    orderSessionManager.getCurrentOrder(
                            request.getCustomerName()
                    );

            // If session is empty, search pending order
            if (currentOrder == null) {

                currentOrder =
                        orderRepository
                                .findFirstByCustomerNameIgnoreCaseAndStatusOrderByIdDesc(
                                        request.getCustomerName(),
                                        "PENDING"
                                )
                                .orElse(null);

                if (currentOrder != null) {

                    orderSessionManager.setCurrentOrder(
                            request.getCustomerName(),
                            currentOrder
                    );
                }
            }

            // No pending order
            if (currentOrder == null) {

                return new VoiceResponse(
                        "NO_ACTIVE_ORDER",
                        "There is no pending order to modify."
                );
            }

            // =====================================================
            // IMPORTANT:
            // Do not allow modification after confirmation
            // =====================================================

            if (!"PENDING".equalsIgnoreCase(
                    currentOrder.getStatus())) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Your order cannot be modified because it is already "
                                + currentOrder.getStatus().toLowerCase()
                                + "."
                );
            }

            List<MenuItem> menuItems =
                    menuItemRepository.findAll();

            MenuItem matchedItem = null;

            for (MenuItem item : menuItems) {

                if (text.contains(
                        item.getName().toLowerCase())) {

                    matchedItem = item;
                    break;
                }
            }

            if (matchedItem == null) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "I could not find the requested menu item."
                );
            }

            if (!matchedItem.isAvailable()) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        matchedItem.getName()
                                + " is currently unavailable."
                );
            }

            int quantity =
                    extractModificationQuantity(
                            text,
                            matchedItem.getName()
                    );

            if (quantity == 0) {
                quantity = 1;
            }

            OrderItem existingItem = null;

            for (OrderItem orderItem :
                    currentOrder.getItems()) {

                if (orderItem.getMenuItem()
                        .getId()
                        .equals(matchedItem.getId())) {

                    existingItem = orderItem;
                    break;
                }
            }

            if (existingItem != null) {

                existingItem.setQuantity(
                        existingItem.getQuantity() + quantity
                );

            } else {

                OrderItem orderItem =
                        new OrderItem();

                orderItem.setOrder(currentOrder);
                orderItem.setMenuItem(matchedItem);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(matchedItem.getPrice());

                currentOrder.getItems().add(orderItem);
            }

            double total =
                    calculateTotal(currentOrder);

            currentOrder.setTotalAmount(total);

            orderRepository.save(currentOrder);

            orderSessionManager.setCurrentOrder(
                    request.getCustomerName(),
                    currentOrder
            );

            return new VoiceResponse(
                    "ORDER_ITEM_ADDED",
                    "Added "
                            + quantity
                            + " "
                            + matchedItem.getName()
                            + ". New total: ₹"
                            + total
                            + "."
            );
        }

        // =========================================================
        // 4. REMOVE ITEM FROM EXISTING ORDER
        // =========================================================

        if (text.contains("remove")
                || text.contains("delete")) {

            Order currentOrder =
                    orderSessionManager.getCurrentOrder(
                            request.getCustomerName()
                    );

            if (currentOrder == null) {

                return new VoiceResponse(
                        "NO_ACTIVE_ORDER",
                        "There is no active order."
                );
            }

            // Do not allow removal after confirmation
            if (!"PENDING".equalsIgnoreCase(
                    currentOrder.getStatus())) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Your order cannot be modified because it is already "
                                + currentOrder.getStatus().toLowerCase()
                                + "."
                );
            }

            List<MenuItem> menuItems =
                    menuItemRepository.findAll();

            MenuItem matchedItem = null;

            for (MenuItem item : menuItems) {

                if (text.contains(
                        item.getName().toLowerCase())) {

                    matchedItem = item;
                    break;
                }
            }

            if (matchedItem == null) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "I could not find the item you want to remove."
                );
            }

            OrderItem itemToRemove = null;

            for (OrderItem orderItem :
                    currentOrder.getItems()) {

                if (orderItem.getMenuItem()
                        .getId()
                        .equals(matchedItem.getId())) {

                    itemToRemove = orderItem;
                    break;
                }
            }

            if (itemToRemove == null) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        matchedItem.getName()
                                + " is not in your current order."
                );
            }

                       int removeQuantity =
                    extractModificationQuantity(
                            text,
                            matchedItem.getName()
                    );

            if (removeQuantity <= 0) {
                removeQuantity = 1;
            }

            int currentQuantity =
                    itemToRemove.getQuantity();

            if (removeQuantity >= currentQuantity) {

                currentOrder.getItems()
                        .remove(itemToRemove);

            } else {

                itemToRemove.setQuantity(
                        currentQuantity - removeQuantity
                );
            }

            double total =
                    calculateTotal(currentOrder);

            currentOrder.setTotalAmount(total);

            orderRepository.save(currentOrder);

            orderSessionManager.setCurrentOrder(
                    request.getCustomerName(),
                    currentOrder
            );

            return new VoiceResponse(
                    "ORDER_ITEM_REMOVED",
                    "Removed "
                            + matchedItem.getName()
                            + ". New total: ₹"
                            + total
                            + "."
            );
        }

        // =========================================================
        // 5. MODIFY ITEM QUANTITY
        // =========================================================

        if (text.contains("change")
                || text.contains("make")
                || text.contains("increase")
                || text.contains("decrease")
                || text.contains("update")) {

            Order currentOrder =
                    orderSessionManager.getCurrentOrder(
                            request.getCustomerName()
                    );

            if (currentOrder == null) {

                return new VoiceResponse(
                        "NO_ACTIVE_ORDER",
                        "There is no active order."
                );
            }

            // Do not allow modification after confirmation
            if (!"PENDING".equalsIgnoreCase(
                    currentOrder.getStatus())) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Your order cannot be modified because it is already "
                                + currentOrder.getStatus().toLowerCase()
                                + "."
                );
            }

            List<MenuItem> menuItems =
                    menuItemRepository.findAll();

            MenuItem matchedItem = null;

            for (MenuItem item : menuItems) {

                if (text.contains(
                        item.getName().toLowerCase())) {

                    matchedItem = item;
                    break;
                }
            }

            if (matchedItem == null) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "I could not find the menu item."
                );
            }

            OrderItem existingItem = null;

            for (OrderItem orderItem :
                    currentOrder.getItems()) {

                if (orderItem.getMenuItem()
                        .getId()
                        .equals(matchedItem.getId())) {

                    existingItem = orderItem;
                    break;
                }
            }

            if (existingItem == null) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        matchedItem.getName()
                                + " is not in your current order."
                );
            }

            int newQuantity =
                    extractModificationQuantity(
                            text,
                            matchedItem.getName()
                    );

            if (newQuantity <= 0) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        "Please specify the new quantity."
                );
            }

            existingItem.setQuantity(
                    newQuantity
            );

            double total =
                    calculateTotal(currentOrder);

            currentOrder.setTotalAmount(total);

            orderRepository.save(currentOrder);

            orderSessionManager.setCurrentOrder(
                    request.getCustomerName(),
                    currentOrder
            );

            return new VoiceResponse(
                    "ORDER_UPDATED",
                    "Updated "
                            + matchedItem.getName()
                            + " to "
                            + newQuantity
                            + ". New total: ₹"
                            + total
                            + "."
            );
        }

        // =========================================================
        // 6. MENU
        // =========================================================

        if (text.contains("menu")
                || text.contains("what do you have")
                || text.contains("what is available")
                || text.contains("available food")) {

            Optional<Restaurant> restaurant =
                    restaurantRepository.findById(
                            request.getRestaurantId()
                    );

            if (restaurant.isEmpty()) {

                return new VoiceResponse(
                        "ERROR",
                        "Restaurant not found."
                );
            }

            List<MenuItem> menuItems =
                    menuItemRepository.findAll();

            StringBuilder message =
                    new StringBuilder();

            message.append("Today's menu: ");

            for (MenuItem item : menuItems) {

                if (item.isAvailable()) {

                    message.append(
                            item.getName()
                    );

                    message.append(" ₹");

                    message.append(
                            item.getPrice()
                    );

                    message.append(", ");
                }
            }

            return new VoiceResponse(
                    "MENU",
                    message.toString()
            );
        }

        // =========================================================
        // =========================================================
// 7. BOOKING
// =========================================================

if (text.contains("book")
        || text.contains("booking")
        || text.contains("reserve")
        || text.contains("reservation")) {

   LocalTime bookingTime;

if (request.getBookingTime() != null
        && !request.getBookingTime().isBlank()) {

    bookingTime = LocalTime.parse(request.getBookingTime());

} else {

    bookingTime = extractBookingTime(text);
}

    if (bookingTime == null) {

        return new VoiceResponse(
                "BOOKING_ERROR",
                "Please provide the booking time."
        );
    }
    Integer numberOfGuests = request.getNumberOfGuests();

    if (numberOfGuests == null || numberOfGuests <= 0) {
        numberOfGuests = extractNumberOfGuests(text);
    }

    if (numberOfGuests == null || numberOfGuests <= 0) {
        return new VoiceResponse(
                "BOOKING_ERROR",
                "Please specify the number of guests, for example: 4 people."
        );
    }

    if (numberOfGuests > 50) {
        return new VoiceResponse(
                "BOOKING_ERROR",
                "Number of guests cannot exceed 50."
        );
    }

LocalDate bookingDate;

if (request.getBookingDate() != null
        && !request.getBookingDate().isBlank()) {

    bookingDate = LocalDate.parse(request.getBookingDate());
    } else if (text.toLowerCase().contains("yesterday")) {

    return new VoiceResponse(
            "BOOKING_ERROR",
            "You cannot book a table for a past date. Please choose today or a future date."
    );

} else if (text.toLowerCase().contains("tomorrow")) {

    bookingDate = LocalDate.now().plusDays(1);

} else if (text.toLowerCase().contains("today")) {

    bookingDate = LocalDate.now();

} else {

    return new VoiceResponse(
            "BOOKING_ERROR",
            "Please specify the booking date, for example: today or tomorrow."
    );
}
boolean exists =
        bookingRepository
                .existsByRestaurantIdAndBookingDateAndBookingTime(
                        request.getRestaurantId(),
                        bookingDate,
                        bookingTime
                );
    if (exists) {

        return new VoiceResponse(
                "BOOKING_ERROR",
                "That time slot is already booked."
        );
    }

    Optional<Restaurant> restaurant =
            restaurantRepository.findById(
                    request.getRestaurantId()
            );

    if (restaurant.isEmpty()) {

        return new VoiceResponse(
                "BOOKING_ERROR",
                "Restaurant not found."
        );
    }

    Booking booking =
            new Booking();

    booking.setRestaurant(
            restaurant.get()
    );

    booking.setCustomerName(
            request.getCustomerName()
    );
    booking.setBookingDate(
        bookingDate
);
   booking.setBookingTime(
        bookingTime
);

booking.setNumberOfGuests(numberOfGuests);

booking.setStatus(
        "CONFIRMED"
);

    bookingRepository.save(
            booking
    );

    return new VoiceResponse(
            "BOOKING_CONFIRMED",
            "Your table has been booked successfully for "
                    + bookingTime
                    + "."
    );
}
        // =========================================================
        // 8. NEW ORDER
        // =========================================================

        Optional<Restaurant> restaurant =
                restaurantRepository.findById(
                        request.getRestaurantId()
                );

        if (restaurant.isEmpty()) {

            return new VoiceResponse(
                    "ERROR",
                    "Restaurant not found."
            );
        }

        List<MenuItem> menuItems =
                menuItemRepository.findAll();

        List<MenuItem> selectedItems =
                new ArrayList<>();

        List<Integer> quantities =
                new ArrayList<>();

        for (MenuItem item : menuItems) {

            String itemName =
                    item.getName().toLowerCase();

            if (text.contains(itemName)) {

                int quantity =
                        extractQuantityForItem(
                                text,
                                itemName,
                                selectedItems
                        );

                // An explicit zero or negative quantity is invalid.
                if (quantity == -1) {
    return new VoiceResponse(
            "ORDER_ERROR",
            "Quantity must be at least 1."
    );
}

if (quantity == -2) {
    return new VoiceResponse(
            "ORDER_ERROR",
            "Quantity must be between 1 and 50."
    );
}
                // If the customer did not specify a quantity, default to one.
                if (quantity == 0) {
                    quantity = 1;
                }

                selectedItems.add(item);

                quantities.add(quantity);
            }
        }

        if (selectedItems.isEmpty()) {

            return new VoiceResponse(
                    "UNKNOWN",
                    "Sorry, I could not understand your request."
            );
        }

        // Check availability
        for (MenuItem item :
                selectedItems) {

            if (!item.isAvailable()) {

                return new VoiceResponse(
                        "ORDER_ERROR",
                        item.getName()
                                + " is currently unavailable."
                );
            }
        }

        // =========================================================
        // CREATE NEW ORDER
        // =========================================================

        Order order =
                new Order();

        order.setRestaurant(
                restaurant.get()
        );

        order.setCustomerName(
                request.getCustomerName()
        );

        order.setStatus(
                "PENDING"
        );

        order.setItems(
                new ArrayList<>()
        );

        double total =
                0;

        StringBuilder orderMessage =
                new StringBuilder();

        orderMessage.append(
                "Your order is ready for confirmation. "
        );

        for (int i = 0;
             i < selectedItems.size();
             i++) {

            MenuItem item =
                    selectedItems.get(i);

            int quantity =
                    quantities.get(i);

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setOrder(
                    order
            );

            orderItem.setMenuItem(
                    item
            );

            orderItem.setQuantity(
                    quantity
            );

            orderItem.setPrice(
                    item.getPrice()
            );

            order.getItems()
                    .add(orderItem);

            double itemTotal =
                    item.getPrice()
                            * quantity;

            total += itemTotal;

            orderMessage.append(
                    quantity
                            + " x "
                            + item.getName()
                            + " ₹"
                            + itemTotal
                            + ". "
            );
        }

        order.setTotalAmount(
                total
        );

        Order savedOrder =
                orderRepository.save(
                        order
                );

        // Save current order in session
        orderSessionManager.setCurrentOrder(
                request.getCustomerName(),
                savedOrder
        );

        orderMessage.append(
                "Total: ₹"
                        + total
                        + ". Please confirm your order."
        );

        return new VoiceResponse(
                "ORDER_PENDING_CONFIRMATION",
                orderMessage.toString()
        );
    }
    // =============================================================
// EXTRACT BOOKING TIME
// =============================================================

private LocalTime extractBookingTime(String text) {

    Pattern timePattern =
            Pattern.compile(
                    "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    Matcher matcher =
            timePattern.matcher(text);

    if (matcher.find()) {

        int hour =
                Integer.parseInt(matcher.group(1));

        int minute =
                matcher.group(2) != null
                        ? Integer.parseInt(matcher.group(2))
                        : 0;

        String amPm =
                matcher.group(3).toLowerCase();

        if (hour < 1 || hour > 12 || minute > 59) {
            return null;
        }

        if (amPm.equals("pm") && hour != 12) {
            hour += 12;
        }

        if (amPm.equals("am") && hour == 12) {
            hour = 0;
        }

        return LocalTime.of(hour, minute);
    }

    return null;
}
    // =============================================================
    // EXTRACT NUMBER OF BOOKING GUESTS
    // =============================================================

    private Integer extractNumberOfGuests(String text) {

        Pattern numericPattern = Pattern.compile(
                "\\b(?:for\\s+)?(\\d+)\\s*(?:people|persons|guests|person|guest)\\b",
                Pattern.CASE_INSENSITIVE
        );

        Matcher numericMatcher = numericPattern.matcher(text);

        if (numericMatcher.find()) {
            try {
                int count = Integer.parseInt(numericMatcher.group(1));
                if (count >= 1 && count <= 50) {
                    return count;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        Pattern wordPattern = Pattern.compile(
                "\\b(?:for\\s+)?(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\\s+(?:people|persons|guests|person|guest)\\b",
                Pattern.CASE_INSENSITIVE
        );

        Matcher wordMatcher = wordPattern.matcher(text);

        if (wordMatcher.find()) {
            return switch (wordMatcher.group(1).toLowerCase()) {
                case "one" -> 1;
                case "two" -> 2;
                case "three" -> 3;
                case "four" -> 4;
                case "five" -> 5;
                case "six" -> 6;
                case "seven" -> 7;
                case "eight" -> 8;
                case "nine" -> 9;
                case "ten" -> 10;
                case "eleven" -> 11;
                case "twelve" -> 12;
                default -> null;
            };
        }

        return null;
    }

    // =============================================================
    // CALCULATE ORDER TOTAL
    // =============================================================

    private double calculateTotal(
            Order order) {

        double total = 0;

        for (OrderItem item :
                order.getItems()) {

            total +=
                    item.getPrice()
                            * item.getQuantity();
        }

        return total;
    }

    // =============================================================
    // EXTRACT MODIFICATION QUANTITY
    // =============================================================

    private int extractModificationQuantity(
        String text,
        String itemName) {

    String item = itemName.toLowerCase();

    // =========================================================
    // 1. "change porotta to 4"
    //    "change porotta into 4"
    //    "make porotta 4"
    // =========================================================

    Pattern changePattern =
            Pattern.compile(
                    "(?:change|make|update|set)\\s+"
                            + Pattern.quote(item)
                            + "\\s+(?:to|into|as)?\\s*(\\d+)"
            );

    Matcher changeMatcher =
            changePattern.matcher(text);

    if (changeMatcher.find()) {

        return Integer.parseInt(
                changeMatcher.group(1)
        );
    }

    // =========================================================
    // 2. Half a dozen
    // =========================================================

    if (text.contains("half a dozen")
            || text.contains("half dozen")) {

        return 6;
    }

    // =========================================================
    // 3. Dozen
    // =========================================================

    if (text.contains("dozen")) {

        return 12;
    }

    // =========================================================
    // 4. Couple
    // =========================================================

    if (text.contains("couple")) {

        return 2;
    }

    // =========================================================
    // 5. Number directly before item
    //    Example: "4 porotta"
    // =========================================================

    Pattern pattern =
            Pattern.compile(
                    "(\\d+)\\s+"
                            + Pattern.quote(item)
            );

    Matcher matcher =
            pattern.matcher(text);

    if (matcher.find()) {

        return Integer.parseInt(
                matcher.group(1)
        );
    }

    // =========================================================
    // 6. Spoken numbers
    // =========================================================

    if (text.contains("one " + item)) {
        return 1;
    }

    if (text.contains("two " + item)) {
        return 2;
    }

    if (text.contains("three " + item)) {
        return 3;
    }

    if (text.contains("four " + item)) {
        return 4;
    }

    if (text.contains("five " + item)) {
        return 5;
    }

    if (text.contains("six " + item)) {
        return 6;
    }

    if (text.contains("seven " + item)) {
        return 7;
    }

    if (text.contains("eight " + item)) {
        return 8;
    }

    if (text.contains("nine " + item)) {
        return 9;
    }

    if (text.contains("ten " + item)) {
        return 10;
    }

    if (text.contains("eleven " + item)) {
        return 11;
    }

    if (text.contains("twelve " + item)) {
        return 12;
    }

    // =========================================================
    // 7. "a porotta" / "an item"
    // =========================================================

    if (text.contains("a " + item)
            || text.contains("an " + item)) {

        return 1;
    }

    return 0;
}

    // =============================================================
    // EXTRACT QUANTITY FOR NEW ORDER ITEM
    // =============================================================

    private int extractQuantityForItem(
            String text,
            String itemName,
            List<MenuItem> matchedItems) {

        String lowerText = text.toLowerCase();
        String item = itemName.toLowerCase();
        String quotedItem = Pattern.quote(item);

        // Check explicit numeric quantities first so that values such as
        // "-2 chicken biriyani" and "0 chicken biriyani" are not accepted.
        Pattern numericPattern = Pattern.compile(
                "(?<!\\w)(-?\\d+)\\s+" + quotedItem + "(?!\\w)"
        );
        Matcher numericMatcher = numericPattern.matcher(lowerText);

        if (numericMatcher.find()) {
            int quantity;
            try {
                quantity = Integer.parseInt(numericMatcher.group(1));
            } catch (NumberFormatException ex) {
                return -1;
            }

           if (quantity < 1) {
    return -1;
}

if (quantity > 50) {
    return -2;
}

            return quantity;
        }

        // Half a dozen
        if (lowerText.contains("half a dozen " + item)
                || lowerText.contains("half dozen " + item)) {
            return 6;
        }

        // Dozen
        if (lowerText.contains("dozen " + item)) {
            return 12;
        }

        // Couple
        if (lowerText.contains("couple of " + item)
                || lowerText.contains("couple " + item)) {
            return 2;
        }

        // Spoken quantities
        String[] numberWords = {
                "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine", "ten", "eleven", "twelve"
        };

        for (int i = 0; i < numberWords.length; i++) {
            Pattern spokenPattern = Pattern.compile(
                    "(?<!\\w)" + numberWords[i] + "\\s+" + quotedItem + "(?!\\w)"
            );
            if (spokenPattern.matcher(lowerText).find()) {
                return i + 1;
            }
        }

        // "a chicken biriyani" / "an item"
        if (lowerText.contains("a " + item)
                || lowerText.contains("an " + item)) {
            return 1;
        }

        // No explicit quantity was provided; the caller defaults to one.
        return 0;
    }

}
