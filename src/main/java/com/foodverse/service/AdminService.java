package com.foodverse.service;

import com.foodverse.dto.admin.PlatformStatsDto;
import com.foodverse.dto.admin.UserDto;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.dto.restaurant.RestaurantDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    Page<UserDto> listUsers(Pageable pageable);
    UserDto toggleUserStatus(Long userId);
    Page<RestaurantDto> listRestaurants(Pageable pageable);
    RestaurantDto toggleRestaurantApproval(Long restaurantId);
    Page<OrderDto> listAllOrders(Pageable pageable);
    PlatformStatsDto getPlatformStats();
}
