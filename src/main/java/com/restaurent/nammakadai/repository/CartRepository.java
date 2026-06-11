package com.restaurent.nammakadai.repository;

import com.restaurent.nammakadai.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c JOIN FETCH c.menuItem WHERE c.user.userId = :userId")
    List<Cart> findByUserUserId(Long userId);

    Optional<Cart> findByUserUserIdAndMenuItemItemId(Long userId, Long itemId);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.userId = :userId")
    void deleteByUserUserId(Long userId);
}
