package com.example.orderservice_choreography.events.failure.inventory;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;

public class InventoryReservedFailedEvent extends Event<InventoryReservedFailedEventData> {
    public InventoryReservedFailedEvent(InventoryReservedFailedEventData eventData) {
        super(EventType.INVENTORY_RESERVATION_FAILED, eventData);
    }
}
