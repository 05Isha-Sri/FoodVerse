package com.foodverse.service;

import com.foodverse.dto.order.CreateOrderRequest;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderDto placeOrder(Long customerId, CreateOrderRequest req);
    OrderDto getOrder(Long customerId, Long orderId);
    Page<OrderDto> listOrders(Long customerId, Pageable pageable);
    OrderDto cancelOrder(Long customerId, Long orderId);
    Page<OrderDto> listRestaurantOrders(Long ownerId, Pageable pageable);
    OrderDto updateRestaurantOrderStatus(Long ownerId, Long orderId, OrderStatus status);
    Page<OrderDto> listAvailableDeliveries(Pageable pageable);
    Page<OrderDto> listWaitingDeliveries(Pageable pageable);
    Page<OrderDto> listAgentOrders(Long agentId, Pageable pageable);
    OrderDto acceptDelivery(Long agentId, Long orderId);
    OrderDto updateDeliveryStatus(Long agentId, Long orderId, String status);
}
