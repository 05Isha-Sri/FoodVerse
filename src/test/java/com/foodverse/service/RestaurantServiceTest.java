package com.foodverse.service;

import com.foodverse.dto.restaurant.FoodItemRequest;
import com.foodverse.dto.restaurant.RestaurantDto;
import com.foodverse.dto.restaurant.RestaurantRequest;
import com.foodverse.exception.BadRequestException;
import com.foodverse.exception.ForbiddenException;
import com.foodverse.model.FoodItem;
import com.foodverse.model.Restaurant;
import com.foodverse.model.User;
import com.foodverse.model.enums.Role;
import com.foodverse.repository.FoodItemRepository;
import com.foodverse.repository.RestaurantRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private User ownerUser;
    private User customerUser;
    private Restaurant sampleRestaurant;

    @BeforeEach
    void setUp() {
        ownerUser = new User(10L, "chef_mario", "pass123", "mario@foodverse.com", Role.RESTAURANT_OWNER, true);
        customerUser = new User(20L, "hungry_sam", "pass123", "sam@foodverse.com", Role.CUSTOMER, true);
        sampleRestaurant = new Restaurant(1L, "Mario Bistro", "Italian dining", ownerUser, true);
    }

    @Test
    void testCreateRestaurantSuccess() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(ownerUser));
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.empty());
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(sampleRestaurant);

        RestaurantDto dto = restaurantService.createRestaurant(10L, new RestaurantRequest("Mario Bistro", "Italian dining"));

        assertNotNull(dto);
        assertEquals("Mario Bistro", dto.name());
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void testCreateRestaurantFailsIfNotOwnerRole() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(customerUser));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                restaurantService.createRestaurant(20L, new RestaurantRequest("Sam Diner", "Fast food")));
        assertTrue(ex.getMessage().contains("Only restaurant owners"));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    void testCreateRestaurantFailsIfAlreadyHasRestaurant() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(ownerUser));
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(sampleRestaurant));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                restaurantService.createRestaurant(10L, new RestaurantRequest("Second Branch", "Another shop")));
        assertTrue(ex.getMessage().contains("already have a restaurant"));
    }

    @Test
    void testAddFoodValidatesPriceGreaterThanZero() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(ownerUser));
        when(restaurantRepository.findByOwnerId(10L)).thenReturn(Optional.of(sampleRestaurant));

        FoodItemRequest zeroPriceRequest = new FoodItemRequest("Free Sample", "Free item", 0.0, true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                restaurantService.addFood(10L, zeroPriceRequest));
        assertTrue(ex.getMessage().contains("price greater than ₹0"));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }
}
