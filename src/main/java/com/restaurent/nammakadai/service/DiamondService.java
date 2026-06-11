package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.entity.Coupon;
import com.restaurent.nammakadai.entity.User;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.CouponRepository;
import com.restaurent.nammakadai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiamondService {

    private static final int DIAMONDS_PER_QUALIFYING_ORDER = 50;
    private static final BigDecimal MIN_ORDER_FOR_DIAMONDS = new BigDecimal("500");
    private static final int DIAMONDS_PER_COUPON = 50;
    private static final BigDecimal COUPON_VALUE = new BigDecimal("5.00");

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void awardDiamonds(User user, BigDecimal orderTotal) {
        int slots = orderTotal.divide(MIN_ORDER_FOR_DIAMONDS, 0, java.math.RoundingMode.FLOOR).intValue();
        if (slots > 0) {
            user.setDiamonds(user.getDiamonds() + slots * DIAMONDS_PER_QUALIFYING_ORDER);
            userRepository.save(user);
        }
    }

    @Transactional
    public Coupon claimCoupon(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getDiamonds() < DIAMONDS_PER_COUPON) {
            throw new IllegalArgumentException("Not enough diamonds. Need " + DIAMONDS_PER_COUPON + ", have " + user.getDiamonds());
        }

        user.setDiamonds(user.getDiamonds() - DIAMONDS_PER_COUPON);
        userRepository.save(user);

        Coupon coupon = new Coupon();
        coupon.setCode("DIAMOND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        coupon.setDescription("Diamond reward coupon — ₹5 off");
        coupon.setDiscountAmount(COUPON_VALUE);
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setIsActive(true);
        coupon.setIsClaimed(false);
        coupon.setUser(user);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(30));
        return couponRepository.save(coupon);
    }

    public int getDiamonds(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getDiamonds();
    }

    public List<Coupon> getMyCoupons(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return couponRepository.findByUserUserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));
        if (!coupon.getIsActive()) throw new IllegalArgumentException("Coupon is inactive");
        if (coupon.getIsClaimed()) throw new IllegalArgumentException("Coupon already used");
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Coupon has expired");
        return coupon;
    }

    @Transactional
    public void markCouponUsed(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));
        coupon.setIsClaimed(true);
        couponRepository.save(coupon);
    }
}
