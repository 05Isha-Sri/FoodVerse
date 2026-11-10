package com.foodverse.dto.admin;

public record PlatformStatsDto(
        long totalUsers,
        long totalRestaurants,
        long totalOrders,
        double totalRevenue
) {}
