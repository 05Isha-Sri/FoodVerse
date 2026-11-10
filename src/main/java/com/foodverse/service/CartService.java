package com.foodverse.service;

import com.foodverse.dto.cart.CartDto;
import com.foodverse.dto.cart.CartItemDto;

public interface CartService {

    CartDto getCurrentCart(Long customerId);

    CartDto addItem(Long customerId, CartItemDto item);

    CartDto updateItem(Long customerId, Long cartItemId, int quantity);

    void removeItem(Long customerId, Long cartItemId);

    void clearCart(Long customerId);
}