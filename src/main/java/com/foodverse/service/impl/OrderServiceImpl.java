package com.foodverse.service.impl;

import com.foodverse.dto.coupon.CouponDto;
import com.foodverse.dto.order.CreateOrderRequest;
import com.foodverse.dto.order.OrderDto;
import com.foodverse.exception.BadRequestException;
import com.foodverse.exception.ForbiddenException;
import com.foodverse.exception.ResourceNotFoundException;
import com.foodverse.model.*;
import com.foodverse.model.enums.OrderStatus;
import com.foodverse.repository.FoodItemRepository;
import com.foodverse.repository.OrderRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.CouponService;
import com.foodverse.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final CouponService couponService;

    @Override
    public OrderDto placeOrder(Long customerId, CreateOrderRequest req) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.items() == null || req.items().isEmpty()) throw new BadRequestException("Order must contain at least one item");

        Restaurant restaurant = null;
        double subtotal = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        Order order = new Order();
        order.setCustomer(customer);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress(req.deliveryAddress().trim());

        for (CreateOrderRequest.OrderItemRequest itemReq : req.items()) {
            if (itemReq.quantity() < 1) throw new BadRequestException("Quantity must be at least 1");
            FoodItem foodItem = foodItemRepository.findById(itemReq.foodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food item not found"));
            if (!foodItem.isAvailable()) throw new BadRequestException("Food item is not available: " + foodItem.getName());
            if (restaurant == null) restaurant = foodItem.getRestaurant();
            else if (foodItem.getRestaurant() != null && !restaurant.getId().equals(foodItem.getRestaurant().getId()))
                throw new BadRequestException("All items in an order must be from the same restaurant");
            double price = foodItem.getPrice() == null ? 0.0 : foodItem.getPrice();
            subtotal += price * itemReq.quantity();
            orderItems.add(new OrderItem(null, order, foodItem, itemReq.quantity(), price));
        }

        order.setRestaurant(restaurant);
        order.getItems().addAll(orderItems);
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            CouponDto coupon = couponService.validateAndApply(customerId, req.couponCode(), subtotal);
            subtotal = Math.max(0.0, subtotal - (coupon.discount() == null ? 0.0 : coupon.discount()));
        }
        double tax = Math.round(subtotal * 0.05 * 100.0) / 100.0;
        double deliveryFee = 50.0;
        double total = Math.round((subtotal + tax + deliveryFee) * 100.0) / 100.0;
        order.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        order.setTax(tax);
        order.setDeliveryFee(deliveryFee);
        order.setTotal(total);
        return toDto(orderRepository.save(order));
    }

    @Override @Transactional(readOnly = true)
    public OrderDto getOrder(Long customerId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getCustomer().getId().equals(customerId)) throw new ForbiddenException("Access denied");
        return toDto(order);
    }

    @Override @Transactional(readOnly = true)
    public Page<OrderDto> listOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toDto);
    }

    @Override
    public OrderDto cancelOrder(Long customerId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getCustomer().getId().equals(customerId)) throw new ForbiddenException("Access denied");
        if (order.getStatus() != OrderStatus.PENDING) throw new BadRequestException("Only pending orders can be cancelled");
        order.setStatus(OrderStatus.CANCELLED);
        return toDto(orderRepository.save(order));
    }

    @Override @Transactional(readOnly = true)
    public Page<OrderDto> listRestaurantOrders(Long ownerId, Pageable pageable) {
        return orderRepository.findByRestaurantOwnerId(ownerId, pageable).map(this::toDto);
    }

    @Override
    public OrderDto updateRestaurantOrderStatus(Long ownerId, Long orderId, OrderStatus status) {
        Order order = findOrder(orderId);
        if (order.getRestaurant() == null || order.getRestaurant().getOwner() == null || !order.getRestaurant().getOwner().getId().equals(ownerId))
            throw new ForbiddenException("You cannot manage this order");
        if (status == OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.PREPARING)
            throw new BadRequestException("Order must be PREPARING before it is ready for pickup");
        if (status == OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING)
            throw new BadRequestException("Only pending orders can be confirmed");
        if (status == OrderStatus.PREPARING && order.getStatus() != OrderStatus.CONFIRMED)
            throw new BadRequestException("Confirm the order before preparing it");
        if (status == OrderStatus.REJECTED && order.getStatus() != OrderStatus.PENDING)
            throw new BadRequestException("Only pending orders can be rejected");
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PREPARING && status != OrderStatus.READY_FOR_PICKUP && status != OrderStatus.REJECTED)
            throw new BadRequestException("Invalid restaurant status transition");
        order.setStatus(status);
        return toDto(orderRepository.save(order));
    }

    @Override @Transactional(readOnly = true)
    public Page<OrderDto> listAvailableDeliveries(Pageable pageable) {
        return orderRepository.findByStatusAndDeliveryAgentIsNull(OrderStatus.READY_FOR_PICKUP, pageable).map(this::toDto);
    }

    @Override @Transactional(readOnly = true)
    public Page<OrderDto> listWaitingDeliveries(Pageable pageable) {
        return orderRepository.findByStatusInAndDeliveryAgentIsNull(
                List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING), pageable).map(this::toDto);
    }

    @Override @Transactional(readOnly = true)
    public Page<OrderDto> listAgentOrders(Long agentId, Pageable pageable) {
        return orderRepository.findByDeliveryAgentId(agentId, pageable).map(this::toDto);
    }

    @Override
    public OrderDto acceptDelivery(Long agentId, Long orderId) {
        User agent = userRepository.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));
        if (agent.getRole() != com.foodverse.model.enums.Role.DELIVERY_AGENT) throw new ForbiddenException("Only delivery agents can accept deliveries");
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP || order.getDeliveryAgent() != null)
            throw new BadRequestException("This delivery is no longer available");
        order.setDeliveryAgent(agent);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        return toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto updateDeliveryStatus(Long agentId, Long orderId, String status) {
        Order order = findOrder(orderId);
        if (order.getDeliveryAgent() == null || !order.getDeliveryAgent().getId().equals(agentId)) throw new ForbiddenException("This delivery is not assigned to you");
        if (!"DELIVERED".equals(status)) throw new BadRequestException("Delivery agents can only mark an active delivery as DELIVERED");
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) throw new BadRequestException("Delivery must be out for delivery");
        order.setStatus(OrderStatus.DELIVERED);
        return toDto(orderRepository.save(order));
    }

    private Order findOrder(Long id) { return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found")); }

    private OrderDto toDto(Order order) {
        List<OrderDto.OrderItemDto> items = order.getItems().stream().map(item -> new OrderDto.OrderItemDto(
                item.getId(), item.getFoodItem() != null ? item.getFoodItem().getId() : null,
                item.getFoodItem() != null ? item.getFoodItem().getName() : "Unknown", item.getQuantity(), item.getPriceAtPurchase())).collect(Collectors.toList());
        return new OrderDto(order.getId(), order.getStatus(), order.getCreatedAt(), order.getSubtotal(), order.getTax(), order.getDeliveryFee(), order.getTotal(),
                order.getDeliveryAddress(), order.getRestaurant() != null ? order.getRestaurant().getName() : "Restaurant", order.getCustomer() != null ? order.getCustomer().getUsername() : "Customer", items);
    }
}
