package com.foodverse.service;

import com.foodverse.dto.order.CreateOrderRequest;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.exception.BadRequestException;
import com.foodverse.model.FoodItem;
import com.foodverse.model.Order;
import com.foodverse.model.Restaurant;
import com.foodverse.model.User;
import com.foodverse.model.enums.OrderStatus;
import com.foodverse.model.enums.Role;
import com.foodverse.repository.FoodItemRepository;
import com.foodverse.repository.OrderRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User customer;
    private User owner;
    private User agent;
    private Restaurant restaurant;
    private FoodItem food1;

    @BeforeEach
    void setUp() {
        customer = new User(1L, "cust1", "pass", "cust@example.com", Role.CUSTOMER, true);
        owner = new User(2L, "owner1", "pass", "owner@example.com", Role.RESTAURANT_OWNER, true);
        agent = new User(3L, "agent1", "pass", "agent@example.com", Role.DELIVERY_AGENT, true);
        restaurant = new Restaurant(10L, "Curry House", "Best curries", owner, true);
        food1 = new FoodItem(100L, "Chicken Tikka", "Spicy", 250.0, true, restaurant);
    }

    @Test
    void testPlaceOrderSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(foodItemRepository.findById(100L)).thenReturn(Optional.of(food1));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            return o;
        });

        CreateOrderRequest req = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(100L, 2)),
                "123 MG Road, Bangalore",
                null
        );

        OrderDto dto = orderService.placeOrder(1L, req);

        assertNotNull(dto);
        assertEquals(500L, dto.id());
        assertEquals(OrderStatus.PENDING, dto.status());
        assertEquals("Curry House", dto.restaurantName());
        // Subtotal = 500, delivery fee = 50, tax = 25 -> total = 575
        assertEquals(575.0, dto.total());
    }

    @Test
    void testPlaceOrderRejectsMultipleRestaurants() {
        Restaurant restaurant2 = new Restaurant(20L, "Pizza Palace", "Italian", owner, true);
        FoodItem food2 = new FoodItem(200L, "Margherita", "Cheesy", 300.0, true, restaurant2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(foodItemRepository.findById(100L)).thenReturn(Optional.of(food1));
        when(foodItemRepository.findById(200L)).thenReturn(Optional.of(food2));

        CreateOrderRequest req = new CreateOrderRequest(
                List.of(
                        new CreateOrderRequest.OrderItemRequest(100L, 1),
                        new CreateOrderRequest.OrderItemRequest(200L, 1)
                ),
                "123 MG Road, Bangalore",
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, req));
        assertTrue(ex.getMessage().contains("must be from the same restaurant"));
    }

    @Test
    void testCancelOrderOnlyPending() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.cancelOrder(1L, 1L));
        assertTrue(ex.getMessage().contains("Only pending orders can be cancelled"));
    }

    @Test
    void testOwnerStatusTransitions() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Confirm
        OrderDto confirmed = orderService.updateRestaurantOrderStatus(2L, 1L, OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, confirmed.status());

        // Prepare
        OrderDto preparing = orderService.updateRestaurantOrderStatus(2L, 1L, OrderStatus.PREPARING);
        assertEquals(OrderStatus.PREPARING, preparing.status());

        // Ready for pickup
        OrderDto ready = orderService.updateRestaurantOrderStatus(2L, 1L, OrderStatus.READY_FOR_PICKUP);
        assertEquals(OrderStatus.READY_FOR_PICKUP, ready.status());
    }

    @Test
    void testAcceptDeliverySuccess() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);

        when(userRepository.findById(3L)).thenReturn(Optional.of(agent));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto dto = orderService.acceptDelivery(3L, 1L);

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, dto.status());
    }
}
