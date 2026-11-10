package com.foodverse.controller;

import com.foodverse.dto.admin.PlatformStatsDto;
import com.foodverse.dto.admin.UserDto;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.dto.restaurant.RestaurantDto;
import com.foodverse.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public PlatformStatsDto getStats() {
        return adminService.getPlatformStats();
    }

    @GetMapping("/users")
    public Page<UserDto> getUsers(Pageable pageable) {
        return adminService.listUsers(pageable);
    }

    @PutMapping("/users/{id}/toggle-status")
    public UserDto toggleUserStatus(@PathVariable Long id) {
        return adminService.toggleUserStatus(id);
    }

    @GetMapping("/restaurants")
    public Page<RestaurantDto> getRestaurants(Pageable pageable) {
        return adminService.listRestaurants(pageable);
    }

    @PutMapping("/restaurants/{id}/approval")
    public RestaurantDto toggleRestaurantApproval(@PathVariable Long id) {
        return adminService.toggleRestaurantApproval(id);
    }

    @GetMapping("/orders")
    public Page<OrderDto> getOrders(Pageable pageable) {
        return adminService.listAllOrders(pageable);
    }
}
