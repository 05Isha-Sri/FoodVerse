package com.foodverse.dto.restaurant;

public record RestaurantDto(

        Long id,

        String name,

        String description,

        boolean approved) {}