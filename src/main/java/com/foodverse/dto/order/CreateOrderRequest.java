package com.foodverse.dto.order;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty List<OrderItemRequest> items,
        @NotBlank String deliveryAddress,
        String couponCode) {

    public record OrderItemRequest(
            @NotNull Long foodItemId,
            @Min(1) int quantity) {}
}
