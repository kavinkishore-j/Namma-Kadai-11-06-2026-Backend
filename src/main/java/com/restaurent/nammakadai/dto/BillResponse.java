package com.restaurent.nammakadai.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillResponse {

    private Long orderId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime orderedAt;
    private String orderStatus;
    private List<BillItem> items;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;       // 2.5%
    private BigDecimal sgst;       // 2.5%
    private BigDecimal grandTotal;
    private BigDecimal discountAmount;
    private String couponCode;
    private String gstNote = "GST @ 5% (CGST 2.5% + SGST 2.5%) as per Indian restaurant regulations";
    private DeliveryAddress deliveryAddress;

    @Data
    public static class DeliveryAddress {
        private String street;
        private String city;
        private String state;
        private String pincode;
        private String landmark;
    }

    @Data
    public static class BillItem {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
