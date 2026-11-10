package com.foodverse.controller;

import com.foodverse.dto.restaurant.*;
import com.foodverse.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService service;

    @GetMapping
    public Page<RestaurantDto> list(Pageable pageable) {
        return service.listAll(pageable);
    }

    @GetMapping("/{id}")
    public RestaurantDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/food-items")
    public Page<FoodItemDto> listFood(@PathVariable Long id,
                                      Pageable pageable) {
        return service.listFoodItems(id, pageable);
    }
}