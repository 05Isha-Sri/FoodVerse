package com.foodverse.service.impl;

import com.foodverse.dto.cart.CartDto;
import com.foodverse.dto.cart.CartItemDto;
import com.foodverse.exception.BadRequestException;
import com.foodverse.exception.ResourceNotFoundException;
import com.foodverse.model.Cart;
import com.foodverse.model.CartItem;
import com.foodverse.model.FoodItem;
import com.foodverse.model.User;
import com.foodverse.repository.CartRepository;
import com.foodverse.repository.FoodItemRepository;
import com.foodverse.repository.UserRepository;
import com.foodverse.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;

    @Override
    public CartDto getCurrentCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        return mapToDto(cart);
    }

    @Override
    public CartDto addItem(Long customerId, CartItemDto item) {
        if (item.quantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        Cart cart = getOrCreateCart(customerId);
        FoodItem foodItem = foodItemRepository.findById(item.foodItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + item.foodItemId()));

        if (!foodItem.isAvailable()) {
            throw new BadRequestException("Food item is not available: " + foodItem.getName());
        }

        Long newRestaurantId = foodItem.getRestaurant() != null ? foodItem.getRestaurant().getId() : null;
        for (CartItem existing : cart.getItems()) {
            Long existingRestaurantId = existing.getFoodItem() != null && existing.getFoodItem().getRestaurant() != null
                    ? existing.getFoodItem().getRestaurant().getId() : null;
            if (newRestaurantId == null || existingRestaurantId == null || !newRestaurantId.equals(existingRestaurantId)) {
                throw new BadRequestException("Your cart can contain items from only one restaurant. Clear the cart before choosing another restaurant.");
            }
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(ci -> ci.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + item.quantity());
        } else {
            CartItem newItem = new CartItem(null, cart, foodItem, item.quantity());
            cart.getItems().add(newItem);
        }

        cart = cartRepository.save(cart);
        return mapToDto(cart);
    }

    @Override
    public CartDto updateItem(Long customerId, Long cartItemId, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be >= 1");
        }
        Cart cart = getOrCreateCart(customerId);
        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getId() != null && ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cartItem.setQuantity(quantity);
        cart = cartRepository.save(cart);
        return mapToDto(cart);
    }

    @Override
    public void removeItem(Long customerId, Long cartItemId) {
        Cart cart = getOrCreateCart(customerId);
        boolean removed = cart.getItems().removeIf(ci -> ci.getId() != null && ci.getId().equals(cartItemId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found with id: " + cartItemId);
        }
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    User customer = userRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + customerId));
                    Cart newCart = new Cart(null, customer, new HashSet<>());
                    return cartRepository.save(newCart);
                });
    }

    private CartDto mapToDto(Cart cart) {
        List<CartItemDto> items = new ArrayList<>();
        double total = 0.0;

        for (CartItem ci : cart.getItems()) {
            FoodItem fi = ci.getFoodItem();
            double price = fi.getPrice() != null ? fi.getPrice() : 0.0;
            total += price * ci.getQuantity();
            items.add(new CartItemDto(
                    ci.getId(),
                    fi.getId(),
                    fi.getName(),
                    ci.getQuantity(),
                    price
            ));
        }

        return new CartDto(cart.getId(), items, Math.round(total * 100.0) / 100.0);
    }
}
