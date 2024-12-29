package com.example.orderservice_choreography.controller;

import com.example.orderservice_choreography.model.OrderRequestDTO;
import com.example.orderservice_choreography.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.SQLException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    @PostMapping("/place")
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequestDTO orderRequest) throws JsonProcessingException, SQLException {
            final var orderId=orderService.placeOrder(orderRequest.getCustomerId(),orderRequest.getItems());
            return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) throws JsonProcessingException {
        orderService.cancelOrder(orderId);
        return new ResponseEntity<>("Order Cancelled successfully!", HttpStatus.OK);
    }
}
