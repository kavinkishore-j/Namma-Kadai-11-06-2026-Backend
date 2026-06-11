package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.dto.CartResponse;
import com.restaurent.nammakadai.entity.Cart;
import com.restaurent.nammakadai.entity.MenuItem;
import com.restaurent.nammakadai.entity.User;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.CartRepository;
import com.restaurent.nammakadai.repository.MenuItemRepository;
import com.restaurent.nammakadai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<CartResponse> getCart(String email) {
        User user = getUser(email);
        return cartRepository.findByUserUserId(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CartResponse addItem(String email, Long itemId, int quantity) {
        User user = getUser(email);
        MenuItem menuItem = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
        if (!menuItem.getAvailable()) {
            throw new IllegalArgumentException("Item not available: " + menuItem.getName());
        }

        Cart cart = cartRepository.findByUserUserIdAndMenuItemItemId(user.getUserId(), itemId)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user);
                    c.setMenuItem(menuItem);
                    c.setQuantity(0);
                    c.setPrice(menuItem.getPrice());
                    return c;
                });

        cart.setQuantity(cart.getQuantity() + quantity);
        cart.setPrice(menuItem.getPrice());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void removeItem(String email, Long itemId) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUserUserIdAndMenuItemItemId(user.getUserId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));
        cartRepository.delete(cart);
    }

    @Transactional
    public CartResponse reduceItem(String email, Long itemId) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUserUserIdAndMenuItemItemId(user.getUserId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));
        if (cart.getQuantity() <= 1) {
            cartRepository.delete(cart);
            return null;
        }
        cart.setQuantity(cart.getQuantity() - 1);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String email) {
        User user = getUser(email);
        cartRepository.deleteByUserUserId(user.getUserId());
    }

    private CartResponse toResponse(Cart cart) {
        CartResponse dto = new CartResponse();
        dto.setCartId(cart.getCartId());
        dto.setQuantity(cart.getQuantity());
        dto.setPrice(cart.getPrice());
        MenuItem mi = cart.getMenuItem();
        dto.setItemId(mi.getItemId());
        dto.setItemName(mi.getName());
        dto.setImageUrl(mi.getImageUrl());
        return dto;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
