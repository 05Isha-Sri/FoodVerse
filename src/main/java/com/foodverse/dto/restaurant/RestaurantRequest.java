package com.foodverse.dto.restaurant;

import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(
        @NotBlank String name,
        String description) {}
