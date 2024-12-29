package com.example.orderservice_choreography.events.success.inventory;

import com.example.orderservice_choreography.model.OrderItemDto;

import java.util.List;

public class InventoryReservedEventData {
    private Long customerId;
    private List<OrderItemDto> reservedItems;
    private String orderId;

    public InventoryReservedEventData(Long customerId, List<OrderItemDto> reservedItems,String orderId) {
        this.customerId = customerId;
        this.reservedItems = reservedItems;
        this.orderId=orderId;
    }

    public InventoryReservedEventData() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemDto> getReservedItems() {
        return reservedItems;
    }

    public void setReservedItems(List<OrderItemDto> reservedItems) {
        this.reservedItems = reservedItems;
    }


}
