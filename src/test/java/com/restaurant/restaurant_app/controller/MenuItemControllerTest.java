
package com.restaurant.restaurant_app.controller;

import com.restaurant.restaurant_app.entity.MenuItem;
import com.restaurant.restaurant_app.repository.MenuItemRepository;
import com.restaurant.restaurant_app.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MenuItemControllerTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private MenuItemController menuItemController;

    @BeforeEach
    void setUp() {
        menuItemController = new MenuItemController(
                menuItemRepository,
                restaurantRepository
        );
    }

    @Test
    void updateAvailabilityWorksForCorrectRestaurant() {
        MenuItem menuItem = new MenuItem();

        when(menuItemRepository.findByIdAndRestaurantId(10L, 1L))
                .thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(menuItem)).thenReturn(menuItem);

        MenuItem result =
                menuItemController.updateAvailability(1L, 10L, false);

        assertSame(menuItem, result);
        assertFalse(result.isAvailable());

        verify(menuItemRepository)
                .findByIdAndRestaurantId(10L, 1L);
        verify(menuItemRepository).save(menuItem);
    }

    @Test
    void updateAvailabilityRejectsItemFromAnotherRestaurant() {
        when(menuItemRepository.findByIdAndRestaurantId(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> menuItemController.updateAvailability(2L, 10L, false)
        );

        verify(menuItemRepository)
                .findByIdAndRestaurantId(10L, 2L);
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }
}
