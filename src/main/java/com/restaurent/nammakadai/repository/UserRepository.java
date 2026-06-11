package com.restaurent.nammakadai.repository;

import com.restaurent.nammakadai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'STAFF'")
    List<User> findAllStaff();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'STAFF' AND u.approvalStatus = 'PENDING'")
    List<User> findPendingStaff();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'STAFF' AND u.enabled = true AND u.approvalStatus = 'APPROVED'")
    List<User> findApprovedStaff();
}
