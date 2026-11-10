package com.foodverse.dto.order;

import com.foodverse.model.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        OrderStatus status,
        LocalDateTime createdAt,
        Double subtotal,
        Double tax,
        Double deliveryFee,
        Double total,
        String deliveryAddress,
        String restaurantName,
        String customerName,
        List<OrderItemDto> items) {

    public record OrderItemDto(
            Long id,
            Long foodItemId,
            String name,
            int quantity,
            Double priceAtPurchase) {}
}
