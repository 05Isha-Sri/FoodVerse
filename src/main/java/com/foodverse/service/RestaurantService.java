package com.foodverse.service;

import com.foodverse.dto.restaurant.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RestaurantService {
    Page<RestaurantDto> listAll(Pageable pageable);
    RestaurantDto getById(Long id);
    Page<FoodItemDto> listFoodItems(Long restaurantId, Pageable pageable);

    RestaurantDto getOwnerRestaurant(Long ownerId);
    RestaurantDto createRestaurant(Long ownerId, RestaurantRequest request);
    RestaurantDto updateRestaurant(Long ownerId, RestaurantRequest request);
    Page<FoodItemDto> listOwnerMenu(Long ownerId, Pageable pageable);
    FoodItemDto addFood(Long ownerId, FoodItemRequest request);
    FoodItemDto updateFood(Long ownerId, Long foodId, FoodItemRequest request);
    void deleteFood(Long ownerId, Long foodId);
}
