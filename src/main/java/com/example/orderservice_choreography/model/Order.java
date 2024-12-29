package com.example.orderservice_choreography.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String orderId;
    private Long customerId;
    private Long deliveryAgentId;

    //Stores the enum as a STRING value
    // Example: If OrderStatus.CREATED is saved, it stores "CREATED" in database
    //If @Enumerated(EnumType.ORDINAL) ,then If OrderStatus.CREATED is saved, it stores 0 in database
    @Enumerated(EnumType.STRING)

    private OrderStatus orderStatus;

    /**
     * Parent (Order) → Child (OrderItem) Relationship
     *
     * Cascade defines: "When I do something to Order, what happens to its OrderItems?"
     *
     * * Individual Cascade Types:
     *  * 1. PERSIST: Save operations cascade to related entities
     *  * 2. MERGE: Related entities are merged when the owning entity is merged
     *  * 3. REMOVE: Related entities are removed when the owning entity is deleted
     *  * 4. REFRESH: Related entities are refreshed when the owning entity is refreshed
     *  * 5. DETACH: Detaches all related entities if a parent entity is detached
     *
     * CascadeType.ALL means:
     * 1. Save Order → Saves all its OrderItems
     * 2. Update Order → Updates all its OrderItems
     * 3. Delete Order → Deletes all its OrderItems
     * 4. Refresh Order → Refreshes all its OrderItems
     *
     *
     *  Strong Relationship (Parent-Child):
     *  OrderItems cannot exist without Order
     *  Use CascadeType.ALL and orphanRemoval=true
     *
     *  CascadeType.ALL with orphanRemoval=true
     *  When deleting order:
     *  DELETE FROM orders WHERE id = ?;
     *  Automatically triggers:
     *  DELETE FROM order_items WHERE order_id = ?;
     *
     *  Without cascade:
     *  Must manually handle related records
     *  DELETE FROM order_items WHERE order_id = ?;
     *  DELETE FROM orders WHERE id = ?;
     *
     *
     *  Weak Relationship:
     *  Customer can exist without Order
     *  Use specific cascades only- CascadeType.PERSIST, CascadeType.MERGE
     *
     *
     * Database Relationship:
     * - One Order can have Many OrderItems
     * - OrderItems cannot exist without an Order (orphanRemoval = true)
     * - Foreign Key: order_id in order_items table references orders(id)
     *
     */
     @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
     private List<OrderItem> orderItems = new ArrayList<>();  // Collection of OrderItems
    @Column(name = "orderDate", nullable = false, updatable = false)
    private LocalDateTime orderDate;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public Long getDeliveryAgentId() {
        return deliveryAgentId;
    }

    public void setDeliveryAgentId(Long deliveryAgentId) {
        this.deliveryAgentId = deliveryAgentId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Order() {
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", customerId=" + customerId +
                ", orderDate=" + orderDate +
                ", deliveryAgentId=" + deliveryAgentId +
                ", orderItems=" + orderItems +
                '}';
    }


    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}

