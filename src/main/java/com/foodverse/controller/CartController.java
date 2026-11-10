package com.foodverse.controller;

import com.foodverse.dto.cart.*;
import com.foodverse.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService service;

    @GetMapping
    public CartDto get(@AuthenticationPrincipal User principal) {
        long userId = Long.parseLong(principal.getUsername());
        return service.getCurrentCart(userId);
    }

    @PostMapping("/items")
    public CartDto add(@AuthenticationPrincipal User principal,
                       @RequestBody CartItemDto item) {
        long userId = Long.parseLong(principal.getUsername());
        return service.addItem(userId, item);
    }

    @PutMapping("/items/{itemId}")
    public CartDto update(@AuthenticationPrincipal User principal,
                          @PathVariable Long itemId,
                          @RequestParam int quantity) {
        long userId = Long.parseLong(principal.getUsername());
        return service.updateItem(userId, itemId, quantity);
    }

    @DeleteMapping("/items/{itemId}")
    public void delete(@AuthenticationPrincipal User principal,
                       @PathVariable Long itemId) {
        long userId = Long.parseLong(principal.getUsername());
        service.removeItem(userId, itemId);
    }

    @DeleteMapping
    public void clear(@AuthenticationPrincipal User principal) {
        long userId = Long.parseLong(principal.getUsername());
        service.clearCart(userId);
    }
}