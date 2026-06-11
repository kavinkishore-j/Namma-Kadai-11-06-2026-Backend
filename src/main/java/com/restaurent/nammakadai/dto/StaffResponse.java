package com.restaurent.nammakadai.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class StaffResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Boolean enabled;
    private String approvalStatus;
    private Double liveLat;
    private Double liveLng;
    private LocalDateTime locationUpdatedAt;
    private LocalDateTime createdAt;
    private Set<String> roles;
}
