package com.example.orderservice_choreography.service;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.success.agent.AgentAssignedEventData;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEventData;
import com.example.orderservice_choreography.model.OutboxEvent;
import com.example.orderservice_choreography.repository.OutBoxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;


@Service
public class OutBoxProcessor {
    private final OutBoxRepository outBoxRepository;
    private final SqsClient sqsClient;
    private final String orderQueueUrl;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(OutBoxProcessor.class);


    public OutBoxProcessor(OutBoxRepository outBoxRepository, SqsClient sqsClient,
                           @Value("${queue.order.url}") String orderQueueUrl,
                           @Qualifier("event-deserializer") ObjectMapper objectMapper) {
        this.outBoxRepository = outBoxRepository;
        this.sqsClient = sqsClient;
        this.orderQueueUrl = orderQueueUrl;
        this.objectMapper=objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        final var pendingOutBoxEvents = outBoxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingOutBoxEvents) {
            try {

                final var orderPlacedEvent=event.getEventData();

                final var orderPlacedEventMessageBody = objectMapper.writeValueAsString(orderPlacedEvent);

                // Send to SQS
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(orderQueueUrl)
                        .messageBody(orderPlacedEventMessageBody)
                        .build());

                // Mark as SENT
                event.setStatus("SENT");
                outBoxRepository.save(event);
            }
            // Case 1: SQS message delivery fails. Since marking the event as SENT happens after sending to SQS,
            // failure to send means the event remains in the PENDING state. Simply log the error.
            // The event will be retried automatically in the next scheduled run.

            // Case 2: SQS message delivery succeeds, but marking the event as SENT in the database fails.
            // In this scenario, the event remains in the PENDING state and will be retried.
            // However, idempotency is implemented in the downstream services (e.g., Inventory),
            // so re-sending the same message will not cause issues. No additional handling is required.

            catch (Exception e) {
                logger.error("Failed to process event with ID: " + event.getId() + " and type: " + event.getEventType(), e);
            }
        }
    }
}
