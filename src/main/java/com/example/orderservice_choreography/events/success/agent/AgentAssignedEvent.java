package com.example.orderservice_choreography.events.success.agent;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.EventType;

public class AgentAssignedEvent extends Event<AgentAssignedEventData> {
    public AgentAssignedEvent(AgentAssignedEventData agentAssignedEventData) {
        super(EventType.AGENT_ASSIGNED, agentAssignedEventData);
    }
}
