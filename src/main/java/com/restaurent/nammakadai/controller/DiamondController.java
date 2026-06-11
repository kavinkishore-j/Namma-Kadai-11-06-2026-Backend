package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.entity.Coupon;
import com.restaurent.nammakadai.service.DiamondService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diamonds")
@RequiredArgsConstructor
public class DiamondController {

    private final DiamondService diamondService;

    @GetMapping
    public ResponseEntity<Map<String, Integer>> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of("diamonds", diamondService.getDiamonds(userDetails.getUsername())));
    }

    @PostMapping("/claim-coupon")
    public ResponseEntity<Coupon> claimCoupon(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(diamondService.claimCoupon(userDetails.getUsername()));
    }

    @GetMapping("/my-coupons")
    public ResponseEntity<List<Coupon>> myCoupons(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(diamondService.getMyCoupons(userDetails.getUsername()));
    }

    @GetMapping("/validate-coupon/{code}")
    public ResponseEntity<Coupon> validateCoupon(@PathVariable String code) {
        return ResponseEntity.ok(diamondService.validateCoupon(code));
    }
}
