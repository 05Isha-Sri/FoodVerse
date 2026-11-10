package com.foodverse.controller;

import com.foodverse.dto.order.OrderDto;
import com.foodverse.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_AGENT')")
public class DeliveryController {
    private final OrderService orderService;

    private long agentId(User principal) { return Long.parseLong(principal.getUsername()); }

    @GetMapping("/available")
    public Page<OrderDto> available(Pageable pageable) {
        return orderService.listAvailableDeliveries(pageable);
    }

    @GetMapping("/waiting")
    public Page<OrderDto> waiting(Pageable pageable) {
        return orderService.listWaitingDeliveries(pageable);
    }

    @GetMapping("/my")
    public Page<OrderDto> my(@AuthenticationPrincipal User principal, Pageable pageable) {
        return orderService.listAgentOrders(agentId(principal), pageable);
    }

    @PutMapping("/{id}/accept")
    public OrderDto accept(@AuthenticationPrincipal User principal, @PathVariable Long id) {
        return orderService.acceptDelivery(agentId(principal), id);
    }

    @PutMapping("/{id}/status")
    public OrderDto status(@AuthenticationPrincipal User principal,
                           @PathVariable Long id,
                           @RequestParam String status) {
        return orderService.updateDeliveryStatus(agentId(principal), id, status);
    }
}
