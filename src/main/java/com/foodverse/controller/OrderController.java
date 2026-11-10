package com.foodverse.controller;

import com.foodverse.dto.order.*;
import com.foodverse.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService service;

    @PostMapping
    public OrderDto place(@AuthenticationPrincipal User principal,
                         @Valid @RequestBody CreateOrderRequest req) {
        long userId = Long.parseLong(principal.getUsername());
        return service.placeOrder(userId, req);
    }

    @GetMapping("/{id}")
    public OrderDto get(@AuthenticationPrincipal User principal,
                        @PathVariable Long id) {
        long userId = Long.parseLong(principal.getUsername());
        return service.getOrder(userId, id);
    }

    @GetMapping("/my")
    public Page<OrderDto> list(@AuthenticationPrincipal User principal,
                               Pageable pageable) {
        long userId = Long.parseLong(principal.getUsername());
        return service.listOrders(userId, pageable);
    }

    @PutMapping("/{id}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal User principal,
                           @PathVariable Long id) {
        long userId = Long.parseLong(principal.getUsername());
        return service.cancelOrder(userId, id);
    }
}