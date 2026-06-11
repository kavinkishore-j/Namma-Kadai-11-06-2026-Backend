package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.dto.BillResponse;
import com.restaurent.nammakadai.dto.OrderRequest;
import com.restaurent.nammakadai.dto.OrderResponse;
import com.restaurent.nammakadai.service.BillService;
import com.restaurent.nammakadai.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final BillService billService;

    @PostMapping
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal UserDetails userDetails,
                                                    @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userDetails.getUsername(), request));
    }

    @PostMapping("/from-cart")
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<OrderResponse> placeOrderFromCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrderFromCart(userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    @GetMapping("/received")
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ResponseEntity<List<OrderResponse>> receivedOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ResponseEntity<List<OrderResponse>> allOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPendingOrder(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        orderService.cancelPendingOrder(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('UPDATE_ORDER_STATUS')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                       @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
    }

    @PostMapping("/{id}/request-delivery-otp")
    @PreAuthorize("hasAuthority('UPDATE_ORDER_STATUS')")
    public ResponseEntity<Map<String, String>> requestDeliveryOtp(@PathVariable Long id) {
        orderService.sendDeliveryOtp(id);
        return ResponseEntity.ok(Map.of("message", "OTP sent to customer"));
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasAuthority('UPDATE_ORDER_STATUS')")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(orderService.confirmDelivery(id, body.get("otp")));
    }

    @GetMapping("/{id}/bill")
    public ResponseEntity<BillResponse> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBill(id));
    }

    @GetMapping("/{id}/bill/pdf")
    public ResponseEntity<byte[]> getBillPdf(@PathVariable Long id) {
        byte[] pdf = billService.generatePdfBill(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "bill-order-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
