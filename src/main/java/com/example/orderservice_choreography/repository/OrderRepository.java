package com.example.orderservice_choreography.repository;

import com.example.orderservice_choreography.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    boolean existsByOrderId(String orderId);
    Optional<Order> findByOrderId(String orderId);


}
