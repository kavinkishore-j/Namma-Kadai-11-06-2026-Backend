package com.restaurent.nammakadai.repository;

import com.restaurent.nammakadai.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserUserId(Long userId);
    List<Address> findByUserEmail(String email);
    Optional<Address> findByOrderOrderId(Long orderId);
}
