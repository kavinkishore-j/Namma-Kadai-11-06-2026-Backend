package com.restaurent.nammakadai.repository;

import com.restaurent.nammakadai.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderOrderIdOrderByUpdatedAtDesc(Long orderId);
}
