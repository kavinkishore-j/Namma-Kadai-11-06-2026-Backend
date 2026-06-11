package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.dto.CartResponse;
import com.restaurent.nammakadai.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cartService.getCart(userDetails.getUsername()));
    }

    @PostMapping("/{itemId}")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long itemId,
                                                @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(cartService.addItem(userDetails.getUsername(), itemId, quantity));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, String>> removeItem(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long itemId) {
        cartService.removeItem(userDetails.getUsername(), itemId);
        return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
    }

    @PatchMapping("/{itemId}/reduce")
    public ResponseEntity<?> reduceItem(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long itemId) {
        CartResponse result = cartService.reduceItem(userDetails.getUsername(), itemId);
        if (result == null) return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}
