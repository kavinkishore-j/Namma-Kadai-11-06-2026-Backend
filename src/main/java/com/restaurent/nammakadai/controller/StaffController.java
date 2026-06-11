package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.dto.OrderResponse;
import com.restaurent.nammakadai.dto.StaffResponse;
import com.restaurent.nammakadai.entity.User;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.OrderRepository;
import com.restaurent.nammakadai.repository.UserRepository;
import com.restaurent.nammakadai.service.EmailService;
import com.restaurent.nammakadai.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StaffResponse>> allStaff() {
        return ResponseEntity.ok(userRepository.findAllStaff().stream().map(this::toStaffResponse).toList());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StaffResponse>> pendingStaff() {
        return ResponseEntity.ok(userRepository.findPendingStaff().stream().map(this::toStaffResponse).toList());
    }

    @GetMapping("/approved")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StaffResponse>> approvedStaff() {
        return ResponseEntity.ok(userRepository.findApprovedStaff().stream().map(this::toStaffResponse).toList());
    }

    @PatchMapping("/{staffId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> approve(@PathVariable Long staffId) {
        User staff = getStaff(staffId);
        staff.setApprovalStatus("APPROVED");
        staff.setEnabled(true);
        userRepository.save(staff);
        try {
            emailService.sendSimpleMail(staff.getEmail(),
                    "Account Approved — NammaKadai",
                    "Hi " + staff.getName() + ",\n\nYour staff account has been approved! You can now login to NammaKadai.");
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Staff approved"));
    }

    @PatchMapping("/{staffId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reject(@PathVariable Long staffId) {
        User staff = getStaff(staffId);
        staff.setApprovalStatus("REJECTED");
        staff.setEnabled(false);
        userRepository.save(staff);
        try {
            emailService.sendSimpleMail(staff.getEmail(),
                    "Account Rejected — NammaKadai",
                    "Hi " + staff.getName() + ",\n\nUnfortunately your staff account registration was not approved.");
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("message", "Staff rejected"));
    }

    @PatchMapping("/assign-order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<OrderResponse> assignOrder(@PathVariable Long orderId,
                                                      @RequestBody Map<String, Long> body) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        Long staffId = body.get("staffId");
        if (staffId == null) {
            order.setAssignedStaff(null);
        } else {
            User staff = getStaff(staffId);
            order.setAssignedStaff(staff);
            try {
                emailService.sendSimpleMail(staff.getEmail(),
                        "New Order Assigned — NammaKadai",
                        "Hi " + staff.getName() + ",\n\nOrder #" + orderId + " has been assigned to you for delivery.\nPlease check the admin panel for details.");
            } catch (Exception ignored) {}
        }
        orderRepository.save(order);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User staff = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(orderService.getOrdersByStaff(staff.getUserId()));
    }

    @PatchMapping("/location")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Map<String, String>> updateLocation(@AuthenticationPrincipal UserDetails userDetails,
                                                               @RequestBody Map<String, Double> body) {
        User staff = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        staff.setLiveLat(body.get("lat"));
        staff.setLiveLng(body.get("lng"));
        staff.setLocationUpdatedAt(LocalDateTime.now());
        userRepository.save(staff);
        return ResponseEntity.ok(Map.of("message", "Location updated"));
    }

    @GetMapping("/location/order/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getStaffLocationForOrder(@PathVariable Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        User staff = order.getAssignedStaff();
        if (staff == null || staff.getLiveLat() == null) {
            return ResponseEntity.noContent().build();
        }
        if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            return ResponseEntity.noContent().build();
        }
        Map<String, Object> result = Map.of(
            "lat", staff.getLiveLat(),
            "lng", staff.getLiveLng(),
            "name", staff.getName(),
            "phone", staff.getPhone() != null ? staff.getPhone() : "",
            "updatedAt", staff.getLocationUpdatedAt() != null ? staff.getLocationUpdatedAt().toString() : ""
        );
        return ResponseEntity.ok(result);
    }

    private StaffResponse toStaffResponse(User user) {
        StaffResponse dto = new StaffResponse();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setEnabled(user.getEnabled());
        dto.setApprovalStatus(user.getApprovalStatus());
        dto.setLiveLat(user.getLiveLat());
        dto.setLiveLng(user.getLiveLng());
        dto.setLocationUpdatedAt(user.getLocationUpdatedAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoles(user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toSet()));
        return dto;
    }

    private User getStaff(Long staffId) {
        User user = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        boolean isStaff = user.getRoles().stream().anyMatch(r -> "STAFF".equals(r.getRoleName()));
        if (!isStaff) throw new IllegalArgumentException("User is not a staff member");
        return user;
    }
}
