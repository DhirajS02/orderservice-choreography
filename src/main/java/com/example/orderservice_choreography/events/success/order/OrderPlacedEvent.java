package com.example.orderservice_choreography.events.success.order;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEventData;

public class OrderPlacedEvent extends Event<OrderPlacedEventData> {
    public OrderPlacedEvent(OrderPlacedEventData orderPlacedEventData) {
        super(EventType.ORDER_PLACED, orderPlacedEventData);
    }

    public OrderPlacedEvent() {
    }
}
