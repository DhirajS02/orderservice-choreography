package com.example.orderservice_choreography.events.failure.order.failed;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;

public class OrderFailedEvent extends Event<OrderFailedEventData> {
    public OrderFailedEvent(OrderFailedEventData eventData) {
        super(EventType.ORDER_FAILED, eventData);
    }
}
