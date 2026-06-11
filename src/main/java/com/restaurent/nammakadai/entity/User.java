package com.restaurent.nammakadai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer diamonds = 0;

    // PENDING | APPROVED | REJECTED — used for STAFF registration approval
    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "APPROVED";

    // Staff live location (updated by staff app periodically)
    @Column(name = "live_lat")
    private Double liveLat;

    @Column(name = "live_lng")
    private Double liveLng;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
