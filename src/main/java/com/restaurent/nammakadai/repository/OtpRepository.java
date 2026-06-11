package com.restaurent.nammakadai.repository;

import com.restaurent.nammakadai.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByEmailOrderByExpiryTimeDesc(String email);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Otp o WHERE o.email = :email")
    void deleteByEmail(String email);
}
