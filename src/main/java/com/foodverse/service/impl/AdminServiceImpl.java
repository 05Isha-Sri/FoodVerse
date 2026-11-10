package com.foodverse.service.impl;

import com.foodverse.dto.admin.PlatformStatsDto;
import com.foodverse.dto.admin.UserDto;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.dto.restaurant.RestaurantDto;
import com.foodverse.exception.ResourceNotFoundException;
import com.foodverse.model.Order;
import com.foodverse.model.Restaurant;
import com.foodverse.model.User;
import com.foodverse.model.enums.OrderStatus;
import com.foodverse.repository.OrderRepository;
import com.foodverse.repository.RestaurantRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserDto);
    }

    @Override
    public UserDto toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(!user.isEnabled());
        return toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantDto> listRestaurants(Pageable pageable) {
        return restaurantRepository.findAll(pageable).map(this::toRestaurantDto);
    }

    @Override
    public RestaurantDto toggleRestaurantApproval(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));
        restaurant.setApproved(!restaurant.isApproved());
        return toRestaurantDto(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toOrderDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalRestaurants = restaurantRepository.count();
        long totalOrders = orderRepository.count();

        double revenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();

        return new PlatformStatsDto(
                totalUsers,
                totalRestaurants,
                totalOrders,
                Math.round(revenue * 100.0) / 100.0
        );
    }

    private UserDto toUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled()
        );
    }

    private RestaurantDto toRestaurantDto(Restaurant restaurant) {
        return new RestaurantDto(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.isApproved()
        );
    }

    private OrderDto toOrderDto(Order order) {
        List<OrderDto.OrderItemDto> items = order.getItems().stream().map(item -> new OrderDto.OrderItemDto(
                item.getId(),
                item.getFoodItem() != null ? item.getFoodItem().getId() : null,
                item.getFoodItem() != null ? item.getFoodItem().getName() : "Unknown",
                item.getQuantity(),
                item.getPriceAtPurchase()
        )).collect(Collectors.toList());

        return new OrderDto(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getSubtotal(),
                order.getTax(),
                order.getDeliveryFee(),
                order.getTotal(),
                order.getDeliveryAddress(),
                order.getRestaurant() != null ? order.getRestaurant().getName() : "Restaurant",
                order.getCustomer() != null ? order.getCustomer().getUsername() : "Customer",
                items
        );
    }
}
