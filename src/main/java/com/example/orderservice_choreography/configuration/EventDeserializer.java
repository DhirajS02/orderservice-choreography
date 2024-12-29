package com.example.orderservice_choreography.configuration;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;
import com.example.orderservice_choreography.events.failure.agent.AgentAssignmentFailedEvent;
import com.example.orderservice_choreography.events.failure.agent.AgentAssignmentFailedEventData;
import com.example.orderservice_choreography.events.failure.inventory.InventoryReservedFailedEvent;
import com.example.orderservice_choreography.events.failure.inventory.InventoryReservedFailedEventData;
import com.example.orderservice_choreography.events.failure.order.cancelled.OrderCancelledEvent;
import com.example.orderservice_choreography.events.failure.order.cancelled.OrderCancelledEventData;
import com.example.orderservice_choreography.events.failure.order.failed.OrderFailedEvent;
import com.example.orderservice_choreography.events.failure.order.failed.OrderFailedEventData;
import com.example.orderservice_choreography.events.success.agent.AgentAssignedEvent;
import com.example.orderservice_choreography.events.success.agent.AgentAssignedEventData;
import com.example.orderservice_choreography.events.success.inventory.InventoryReservedEvent;
import com.example.orderservice_choreography.events.success.inventory.InventoryReservedEventData;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEvent;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEventData;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom JSON deserializer for handling different types of events in the order processing system.
 * This deserializer converts JSON messages into specific event types based on the eventType field.
 *
 * Supports the following event types:
 * - Order events (PLACED, FAILED, CANCELLED)
 * - Inventory events (RESERVED, RESERVATION_FAILED)
 * - Agent events (ASSIGNED, ASSIGNMENT_FAILED)
 */
public class EventDeserializer extends JsonDeserializer<Event<?>> {
    /**
     * Deserializes JSON input into specific Event objects based on the eventType.
     *
     * @param jp JsonParser that provides access to JSON content
     * @param ctxt Context for deserialization processing
     * @return Event<?> A concrete implementation of Event based on the eventType
     * @throws IOException if there's an error reading JSON content
     */
    @Override
    public Event<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);
        EventType eventType = EventType.valueOf(node.get("eventType").asText());

        return switch (eventType) {
            case ORDER_PLACED -> new OrderPlacedEvent(codec.treeToValue(node.get("data"), OrderPlacedEventData.class));
            case INVENTORY_RESERVED ->
                    new InventoryReservedEvent(codec.treeToValue(node.get("data"), InventoryReservedEventData.class));
            case AGENT_ASSIGNED ->
                    new AgentAssignedEvent(codec.treeToValue(node.get("data"), AgentAssignedEventData.class));
            case AGENT_ASSIGNMENT_FAILED ->
                new AgentAssignmentFailedEvent(codec.treeToValue(node.get("data"), AgentAssignmentFailedEventData.class));
            case ORDER_FAILED ->
                    new OrderFailedEvent(codec.treeToValue(node.get("data"), OrderFailedEventData.class));
            case ORDER_CANCELLED ->
                    new OrderCancelledEvent(codec.treeToValue(node.get("data"), OrderCancelledEventData.class));
            case INVENTORY_RESERVATION_FAILED ->
                    new InventoryReservedFailedEvent(codec.treeToValue(node.get("data"), InventoryReservedFailedEventData.class));

        };
    }
}
