package com.example.orderservice_choreography.events.failure.order.cancelled;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;

public class OrderCancelledEvent extends Event<OrderCancelledEventData> {
    public OrderCancelledEvent(OrderCancelledEventData eventData) {
        super(EventType.ORDER_CANCELLED, eventData);
    }

}
