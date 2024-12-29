package com.example.orderservice_choreography.events.success.agent;

import java.util.List;
import com.example.orderservice_choreography.model.OrderItemDto;

public class AgentAssignedEventData {
    private Long customerId;
    private List<OrderItemDto> orderItems;
    private Long agentId;
    private String orderId;

    public AgentAssignedEventData(Long customerId, List<OrderItemDto> orderItems, Long agentId, String orderId) {
        this.customerId = customerId;
        this.orderItems = orderItems;
        this.agentId = agentId;
        this.orderId = orderId;
    }

    public AgentAssignedEventData() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemDto> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDto> orderItems) {
        this.orderItems = orderItems;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}

