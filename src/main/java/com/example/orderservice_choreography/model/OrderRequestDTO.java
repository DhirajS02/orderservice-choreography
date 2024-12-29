package com.example.orderservice_choreography.model;

import java.util.List;

/**
 * This class represents a request for creating an order.
 * It contains the customer ID and a list of order items.
 */
public class OrderRequestDTO {
    private Long customerId;
    private List<OrderItemDto> items;

    public OrderRequestDTO(Long customerId, List<OrderItemDto> items) {
        this.customerId = customerId;
        this.items = items;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public OrderRequestDTO() {
    }
}
