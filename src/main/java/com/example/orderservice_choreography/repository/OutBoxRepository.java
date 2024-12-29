package com.example.orderservice_choreography.repository;

import com.example.orderservice_choreography.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutBoxRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findByStatus(String status);
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);


}
