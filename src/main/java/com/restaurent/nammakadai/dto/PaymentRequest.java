package com.restaurent.nammakadai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    private Long orderId;

    @NotBlank
    private String paymentMethod;

    private BigDecimal amount;

    // address fields
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;

    // Optional coupon code
    private String couponCode;

    // For direct checkout (no pre-created order)
    private Boolean fromCart;

    // Customer's GPS coordinates at time of order
    private Double latitude;
    private Double longitude;
}
