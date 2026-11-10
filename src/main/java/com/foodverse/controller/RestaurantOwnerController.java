package com.foodverse.controller;

import com.foodverse.dto.order.OrderDto;
import com.foodverse.dto.restaurant.*;
import com.foodverse.model.enums.OrderStatus;
import com.foodverse.service.OrderService;
import com.foodverse.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerController {
    private final RestaurantService restaurantService;
    private final OrderService orderService;

    private long ownerId(User principal) { return Long.parseLong(principal.getUsername()); }

    @GetMapping("/restaurant")
    public RestaurantDto restaurant(@AuthenticationPrincipal User principal) {
        return restaurantService.getOwnerRestaurant(ownerId(principal));
    }

    @PostMapping("/restaurant")
    public RestaurantDto createRestaurant(@AuthenticationPrincipal User principal,
                                           @Valid @RequestBody RestaurantRequest request) {
        return restaurantService.createRestaurant(ownerId(principal), request);
    }

    @PutMapping("/restaurant")
    public RestaurantDto updateRestaurant(@AuthenticationPrincipal User principal,
                                           @Valid @RequestBody RestaurantRequest request) {
        return restaurantService.updateRestaurant(ownerId(principal), request);
    }

    @GetMapping("/menu")
    public Page<FoodItemDto> menu(@AuthenticationPrincipal User principal, Pageable pageable) {
        return restaurantService.listOwnerMenu(ownerId(principal), pageable);
    }

    @PostMapping("/menu")
    public FoodItemDto addFood(@AuthenticationPrincipal User principal,
                               @Valid @RequestBody FoodItemRequest request) {
        return restaurantService.addFood(ownerId(principal), request);
    }

    @PutMapping("/menu/{id}")
    public FoodItemDto updateFood(@AuthenticationPrincipal User principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody FoodItemRequest request) {
        return restaurantService.updateFood(ownerId(principal), id, request);
    }

    @DeleteMapping("/menu/{id}")
    public void deleteFood(@AuthenticationPrincipal User principal, @PathVariable Long id) {
        restaurantService.deleteFood(ownerId(principal), id);
    }

    @GetMapping("/orders")
    public Page<OrderDto> orders(@AuthenticationPrincipal User principal, Pageable pageable) {
        return orderService.listRestaurantOrders(ownerId(principal), pageable);
    }

    @PutMapping("/orders/{id}/status")
    public OrderDto updateOrderStatus(@AuthenticationPrincipal User principal,
                                      @PathVariable Long id,
                                      @RequestParam OrderStatus status) {
        return orderService.updateRestaurantOrderStatus(ownerId(principal), id, status);
    }
}
