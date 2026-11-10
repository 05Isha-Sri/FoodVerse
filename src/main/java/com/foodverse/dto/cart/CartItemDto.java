package com.foodverse.dto.cart;

public record CartItemDto(

        Long id,

        Long foodItemId,

        String name,

        int quantity,

        Double price) {}