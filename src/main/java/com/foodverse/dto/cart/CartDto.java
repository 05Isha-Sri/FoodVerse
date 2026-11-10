package com.foodverse.dto.cart;

import java.util.List;

public record CartDto(

        Long id,

        List<CartItemDto> items,

        Double total) {}