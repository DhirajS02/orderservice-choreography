package com.example.orderservice_choreography.model;

import jakarta.persistence.*;

/**
 * Represents an item within an order.
 * Demonstrates the many side of the One-to-Many relationship.
 */
@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    /**
     * Reference to the parent Order.
     *
     * Implementation Note:
     * - Creates order_id column in order_items table
     * - @ManyToOne indicates many OrderItems can belong to one Order
     * - @JoinColumn specifies the foreign key column
     * - nullable = false requires each OrderItem to have an Order
     */
    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "orderId")
    private Order order;  // Reference to the Order

    private String itemName;
    private int quantity;

    public OrderItem(Long orderItemId, Order order, String itemName, int quantity) {
        this.orderItemId = orderItemId;
        this.order = order;
        this.itemName = itemName;
        this.quantity = quantity;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public OrderItem() {
    }
}

