package com.restaurent.nammakadai.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartResponse {
    private Long cartId;
    private Long itemId;
    private String itemName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price;
}
