package com.foodverse.service.impl;

import com.foodverse.dto.restaurant.*;
import com.foodverse.exception.BadRequestException;
import com.foodverse.exception.ForbiddenException;
import com.foodverse.exception.ResourceNotFoundException;
import com.foodverse.model.FoodItem;
import com.foodverse.model.Restaurant;
import com.foodverse.model.User;
import com.foodverse.model.enums.Role;
import com.foodverse.repository.FoodItemRepository;
import com.foodverse.repository.RestaurantRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantDto> listAll(Pageable pageable) {
        return restaurantRepository.findByApprovedTrue(pageable).map(this::toRestaurantDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getById(Long id) {
        return toRestaurantDto(restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemDto> listFoodItems(Long restaurantId, Pageable pageable) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
        }
        return foodItemRepository.findByRestaurantId(restaurantId, pageable).map(this::toFoodDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getOwnerRestaurant(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .map(this::toRestaurantDto)
                .orElseThrow(() -> new ResourceNotFoundException("No restaurant has been created for this owner yet"));
    }

    @Override
    public RestaurantDto createRestaurant(Long ownerId, RestaurantRequest request) {
        User owner = getOwner(ownerId);
        if (restaurantRepository.findByOwnerId(ownerId).isPresent()) {
            throw new BadRequestException("You already have a restaurant");
        }
        if (request.name() == null || request.name().trim().isBlank()) {
            throw new BadRequestException("Restaurant name is required");
        }
        Restaurant restaurant = new Restaurant(null, request.name().trim(), request.description(), owner, true);
        return toRestaurantDto(restaurantRepository.save(restaurant));
    }

    @Override
    public RestaurantDto updateRestaurant(Long ownerId, RestaurantRequest request) {
        Restaurant restaurant = getOwnerRestaurantEntity(ownerId);
        if (request.name() == null || request.name().trim().isBlank()) {
            throw new BadRequestException("Restaurant name is required");
        }
        restaurant.setName(request.name().trim());
        restaurant.setDescription(request.description());
        return toRestaurantDto(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodItemDto> listOwnerMenu(Long ownerId, Pageable pageable) {
        Restaurant restaurant = getOwnerRestaurantEntity(ownerId);
        return foodItemRepository.findByRestaurantId(restaurant.getId(), pageable).map(this::toFoodDto);
    }

    @Override
    public FoodItemDto addFood(Long ownerId, FoodItemRequest request) {
        Restaurant restaurant = getOwnerRestaurantEntity(ownerId);
        if (request.name() == null || request.name().trim().isBlank() || request.price() == null || request.price() <= 0) {
            throw new BadRequestException("Food name and a price greater than ₹0 are required");
        }
        FoodItem item = new FoodItem(null, request.name().trim(), request.description(), request.price(), request.available(), restaurant);
        return toFoodDto(foodItemRepository.save(item));
    }

    @Override
    public FoodItemDto updateFood(Long ownerId, Long foodId, FoodItemRequest request) {
        FoodItem item = getOwnedFood(ownerId, foodId);
        if (request.name() == null || request.name().trim().isBlank() || request.price() == null || request.price() <= 0) {
            throw new BadRequestException("Food name and a price greater than ₹0 are required");
        }
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setAvailable(request.available());
        return toFoodDto(foodItemRepository.save(item));
    }

    @Override
    public void deleteFood(Long ownerId, Long foodId) {
        FoodItem item = getOwnedFood(ownerId, foodId);
        foodItemRepository.delete(item);
    }

    private User getOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        if (owner.getRole() != Role.RESTAURANT_OWNER) {
            throw new ForbiddenException("Only restaurant owners can manage restaurants");
        }
        return owner;
    }

    private Restaurant getOwnerRestaurantEntity(Long ownerId) {
        getOwner(ownerId);
        return restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Create your restaurant before managing the menu"));
    }

    private FoodItem getOwnedFood(Long ownerId, Long foodId) {
        Restaurant restaurant = getOwnerRestaurantEntity(ownerId);
        FoodItem item = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found"));
        if (item.getRestaurant() == null || !restaurant.getId().equals(item.getRestaurant().getId())) {
            throw new ForbiddenException("You cannot modify this food item");
        }
        return item;
    }

    private RestaurantDto toRestaurantDto(Restaurant restaurant) {
        return new RestaurantDto(restaurant.getId(), restaurant.getName(), restaurant.getDescription(), restaurant.isApproved());
    }

    private FoodItemDto toFoodDto(FoodItem item) {
        return new FoodItemDto(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.isAvailable());
    }
}
