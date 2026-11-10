package com.foodverse.service;

import com.foodverse.dto.admin.PlatformStatsDto;
import com.foodverse.dto.admin.UserDto;
import com.foodverse.dto.restaurant.RestaurantDto;
import com.foodverse.model.Order;
import com.foodverse.model.Restaurant;
import com.foodverse.model.User;
import com.foodverse.model.enums.OrderStatus;
import com.foodverse.model.enums.Role;
import com.foodverse.repository.OrderRepository;
import com.foodverse.repository.RestaurantRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void testToggleUserStatus() {
        User user = new User(1L, "john", "pass", "john@example.com", Role.CUSTOMER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = adminService.toggleUserStatus(1L);

        assertFalse(dto.enabled());
        verify(userRepository).save(user);
    }

    @Test
    void testToggleRestaurantApproval() {
        Restaurant restaurant = new Restaurant(1L, "Tasty Bites", "Desc", null, false);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantDto dto = adminService.toggleRestaurantApproval(1L);

        assertTrue(dto.approved());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void testGetPlatformStats() {
        when(userRepository.count()).thenReturn(5L);
        when(restaurantRepository.count()).thenReturn(2L);
        when(orderRepository.count()).thenReturn(3L);

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus(OrderStatus.DELIVERED);
        deliveredOrder.setTotal(350.0);

        Order pendingOrder = new Order();
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotal(200.0);

        when(orderRepository.findAll()).thenReturn(List.of(deliveredOrder, pendingOrder));

        PlatformStatsDto stats = adminService.getPlatformStats();

        assertEquals(5L, stats.totalUsers());
        assertEquals(2L, stats.totalRestaurants());
        assertEquals(3L, stats.totalOrders());
        assertEquals(350.0, stats.totalRevenue());
    }
}
