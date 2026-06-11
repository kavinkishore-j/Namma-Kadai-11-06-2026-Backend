package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.entity.Address;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Address> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
            addressRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No address for order: " + orderId))
        );
    }

    @GetMapping("/my/latest")
    public ResponseEntity<Address> getLatest(@AuthenticationPrincipal UserDetails userDetails) {
        List<Address> addresses = addressRepository.findByUserEmail(userDetails.getUsername());
        return addresses.stream()
                .max(Comparator.comparing(a -> a.getAddressId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Address>> all() {
        return ResponseEntity.ok(addressRepository.findAll());
    }
}
