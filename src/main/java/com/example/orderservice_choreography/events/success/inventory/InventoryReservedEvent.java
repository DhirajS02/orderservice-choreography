package com.example.orderservice_choreography.events.success.inventory;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;

public class InventoryReservedEvent extends Event<InventoryReservedEventData> {
    public InventoryReservedEvent(InventoryReservedEventData inventoryReservedData)
    {
        super(EventType.INVENTORY_RESERVED,inventoryReservedData);
    }
}
