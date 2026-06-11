package com.restaurent.nammakadai.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal grandTotal;
    private BigDecimal discountAmount;
    private String couponCode;
    private LocalDateTime createdAt;

    // Flat user info — no proxy
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Flat assigned-staff info — no proxy
    private Long assignedStaffId;
    private String assignedStaffName;
    private String assignedStaffPhone;

    private List<OrderItemResponse> orderItems;
}
