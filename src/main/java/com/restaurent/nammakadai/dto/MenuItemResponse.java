package com.restaurent.nammakadai.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MenuItemResponse {
    private Long itemId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean available;
    private String imageUrl;
    private LocalDateTime createdAt;
}
