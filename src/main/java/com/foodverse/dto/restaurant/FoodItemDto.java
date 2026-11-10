package com.foodverse.dto.restaurant;

public record FoodItemDto(

        Long id,

        String name,

        String description,

        Double price,

        boolean available) {}