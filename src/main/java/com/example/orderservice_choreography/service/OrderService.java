package com.example.orderservice_choreography.service;

import com.example.orderservice_choreography.events.Event;
import com.example.orderservice_choreography.events.failure.agent.AgentAssignmentFailedEventData;
import com.example.orderservice_choreography.events.failure.inventory.InventoryReservedFailedEventData;
import com.example.orderservice_choreography.events.failure.order.cancelled.OrderCancelledEvent;
import com.example.orderservice_choreography.events.failure.order.cancelled.OrderCancelledEventData;
import com.example.orderservice_choreography.events.failure.order.failed.OrderFailedEvent;
import com.example.orderservice_choreography.events.failure.order.failed.OrderFailedEventData;
import com.example.orderservice_choreography.events.success.agent.AgentAssignedEventData;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEvent;
import com.example.orderservice_choreography.events.success.order.OrderPlacedEventData;
import com.example.orderservice_choreography.exceptions.EventParsingException;
import com.example.orderservice_choreography.exceptions.OrderNotFoundException;
import com.example.orderservice_choreography.model.*;
import com.example.orderservice_choreography.repository.OrderRepository;
import com.example.orderservice_choreography.repository.OutBoxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderService {

    private final SqsClient sqsClient;
    private final String agentQueueUrl;
    private final String agentFailureQueueUrl;
    private final String orderFailedQueueUrl;
    private final String inventoryFailedQueueUrl;

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final String orderCancelledTopicArn;
    private final OutBoxRepository outBoxRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(SqsClient sqsClient,
                        @Value("${queue.agent.url}") String agentQueueUrl,
                        @Value("${queue.agent.failed.url}") String agentFailureQueueUrl,
                        @Value("${queue.order.failed.url}") String orderFailedQueueUrl,
                        OrderRepository orderRepository,
                        @Qualifier("event-deserializer") ObjectMapper objectMapper,
                        @Value("${queue.inventory.failed.url}") String inventoryFailedQueueUrl,
                        SnsClient snsClient,
                        @Value("${queue.order.cancelled.topic.arn}") String orderCancelledTopicArn,
                        OutBoxRepository outBoxRepository) {
        this.sqsClient = sqsClient;
        this.agentQueueUrl = agentQueueUrl;
        this.agentFailureQueueUrl = agentFailureQueueUrl;
        this.orderFailedQueueUrl = orderFailedQueueUrl;
        this.inventoryFailedQueueUrl = inventoryFailedQueueUrl;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.orderCancelledTopicArn = orderCancelledTopicArn;
        this.outBoxRepository = outBoxRepository;
    }

    @Transactional
    public String placeOrder(Long customerId, List<OrderItemDto> orderItemDtos) {

        logger.info("Order Placement Request Received. An orderID needs to be generated");

        //Creating OrderId(UUID)
        final var orderId = generateOrderId();

        logger.info("Order created: id={}, status=PENDING, agentId=null", orderId);
        final var order = getOrder(customerId, orderItemDtos, orderId);
        orderRepository.save(order);

        logger.info("Order generated: id={}", orderId);

        final var orderPlacedEvent = getOrderPlacedEvent(customerId, orderItemDtos, orderId);
        logger.info("OrderPlacedEvent created: orderId={}, items={}, destination=inventory-queue, customerId={}",
                orderId,
                orderItemDtos,
                customerId);


        //Creating OutBox object using orderPlacedEvent data
        final var outboxEvent = createOutBoxEvent(orderPlacedEvent);
        outBoxRepository.save(outboxEvent);
        logger.info("Outbox event created: orderId={}, eventType=ORDER_PLACED", orderId);

        return order.getOrderId();
    }

    /**
     * Receive Event from sqs sent by AgentService on successful agent assignment.Pooling every 10 seconds
     *
     * Long Polling vs Short Polling:
     * - Short Polling: Client repeatedly requests data at fixed intervals. Server responds immediately
     *   even if no data is available. Can result in empty responses and higher costs.
     *
     * - Long Polling: Server holds the connection open (waitTimeSeconds=10) until:
     *   a) Data becomes available
     *   b) Timeout period is reached
     *   This reduces empty responses and AWS costs.
     *
     */
    @Scheduled(fixedRate = 20000, initialDelay = 20000) // Poll every 10 seconds
    public void onAgentAssignedEvent() {
        AtomicReference<String> orderId = new AtomicReference<>();

        logger.info("Listening to agent assigned event");

        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(agentQueueUrl)
                .maxNumberOfMessages(10)//Receive 10 message to reduce cost, by this can receive 10 message at a time
                .waitTimeSeconds(10)//Long polling,  connection remains open for the specified duration (in your case 10 seconds) or until data becomes available, whichever comes first.
                .build();

        final var messages = sqsClient.receiveMessage(receiveRequest).messages();

        messages.forEach(message -> {
            logger.info("Received event: AgentAssignedEvent with message body = {}",message.body());
            try {
                orderId.set(processAgentAssignedEvent(message));
                sqsClient.deleteMessage(builder -> builder.queueUrl(agentQueueUrl).receiptHandle(message.receiptHandle()));
                logger.info("AgentAssignedEvent deleted: orderId={}", orderId);
            } catch (EventParsingException e) {
                logger.error("Error processing AgentAssignedEvent message = {} . OrderID= {}", message.body(),orderId, e);
            }
        });
    }

    /**
     * Processes failed agent assignment events from SQS queue.
     * Polls the queue every 20 seconds for messages and handles them accordingly.
     *
     * The method:
     * 1. Receives messages from the agent failure queue
     * 2. Processes each message
     * 3. Deletes successfully processed messages from the queue
     * 4. Handles any errors during message processing
     */
    @Scheduled(fixedRate = 20000, initialDelay = 20000) // Poll every 10 seconds
    public void onAgentAssignmentFailed(){
        AtomicReference<String> orderId = new AtomicReference<>();
        logger.info("Listening to failed agent assigned events");

        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(agentFailureQueueUrl)
                .waitTimeSeconds(10)
                .maxNumberOfMessages(10)
                .build();

        final var messages = sqsClient.receiveMessage(receiveRequest).messages();
        messages.forEach(message -> {
            logger.info("Received event: AgentAssignmentFailed with message body = {}",message.body());
            try {
                orderId.set(processEventFailedAgentAssigned(message));
                sqsClient.deleteMessage(builder -> builder.queueUrl(agentFailureQueueUrl).receiptHandle(message.receiptHandle()));
               logger.info("AgentAssignmentFailed deleted: orderId={}", orderId);
            } catch (EventParsingException e) {
                logger.error("Error processing AgentAssignmentFailedEvent message = {} . OrderID= {}", message.body(),orderId, e);
            }
        });
    }

    @Scheduled(fixedRate = 20000, initialDelay = 20000) // Poll every 10 seconds
    public void onInventoryReservationFailed(){
        AtomicReference<String> orderId = new AtomicReference<>();
        logger.info("Listening to Inventory reservation Failed");

        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(inventoryFailedQueueUrl)
                .waitTimeSeconds(10)
                .maxNumberOfMessages(10)
                .build();

        final var messages = sqsClient.receiveMessage(receiveRequest).messages();
        messages.forEach(message -> {
            logger.info("Received event: InventoryReservedFailed with message body = {}",message.body());
            try {
                orderId.set(processInventoryReservationFailed(message));
                sqsClient.deleteMessage(builder -> builder.queueUrl(inventoryFailedQueueUrl).receiptHandle(message.receiptHandle()));
                logger.info("InventoryReservedFailed deleted: orderId={}", orderId);
            } catch (EventParsingException e) {
                logger.error("Error processing InventoryReservedFailedEvent message = {} . OrderID= {}", message.body(),orderId, e);
            }
        });
    }

    //Processing Successful Agent assigned event
    private String processAgentAssignedEvent(Message eventMessage) throws EventParsingException {
        String orderId = null;
        try {
            logger.debug("Starting to process agent assignment: messageId={}", eventMessage.messageId());
            // Deserialize the event message into AgentAssignedEvent object
            final var agentAssignmentEvent = objectMapper.readValue(
                    eventMessage.body(),
                    new TypeReference<Event<AgentAssignedEventData>>() {
                    }
            );

            // Extract event data containing agent and order details
            final var agentCustomerAndOrderData = agentAssignmentEvent.getData();
            orderId=agentCustomerAndOrderData.getOrderId();

            // Find and update the order if it exists and is in PENDING state
            String finalOrderId = orderId;
            orderRepository.findByOrderId(orderId)
                    .filter(order -> OrderStatus.ORDER_PENDING.equals(order.getOrderStatus())) // Only proceed if status is "PENDING"
                    .ifPresent(order -> {
                        logger.info("Processing order assignment: orderId={}, currentStatus={}, agentId={}",
                                finalOrderId,
                                order.getOrderStatus(),
                                agentCustomerAndOrderData.getAgentId());                        // Update status to "CREATED" and set agentId
                        order.setOrderStatus(OrderStatus.ORDER_CREATED);
                        order.setDeliveryAgentId(agentCustomerAndOrderData.getAgentId());
                        orderRepository.save(order);
                    });

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AgentAssigned event with message ID: {}. Error: {}", eventMessage.messageId(), e.getMessage(), e);
            throw new EventParsingException("Failed to parse AgentAssigned event: " + e.getMessage(), e);
        }
        return orderId;
    }


    private String processEventFailedAgentAssigned(Message eventMessage) throws EventParsingException {
        String orderId = null;
        try {
            final var agentAssignedFailedEventDataEvent = objectMapper.readValue(
                    eventMessage.body(),
                    new TypeReference<Event<AgentAssignmentFailedEventData>>() {
                    }
            );
            final var agentCustomerAndOrderData = agentAssignedFailedEventDataEvent.getData();
            orderId=agentCustomerAndOrderData.getOrderId();

            // Update the order status to FAILED in the database
            orderRepository.findByOrderId(agentCustomerAndOrderData.getOrderId())
                    .filter(order -> OrderStatus.ORDER_PENDING.equals(order.getOrderStatus())) // Ensure order is still in "PENDING" state
                    .ifPresent(order -> {
                        logger.info("Updating order status to FAILED for orderId: {}", order.getOrderId());
                        order.setOrderStatus(OrderStatus.ORDER_FAILED);
                        orderRepository.save(order);
                    });

            logger.info("Order set as failed, as agent assigned failed for Order ID= {}",agentCustomerAndOrderData.getOrderId());

            final var orderFailedEventData = new OrderFailedEventData(
                    agentCustomerAndOrderData.getOrderId(),
                    agentCustomerAndOrderData.getCustomerId(),
                    agentCustomerAndOrderData.getOrderItems()
            );

            final var orderFailedEvent = new OrderFailedEvent(orderFailedEventData);
            final var failedDataMessageBody = objectMapper.writeValueAsString(orderFailedEvent);


            // Send the event to SQS
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(orderFailedQueueUrl)
                    .messageBody(failedDataMessageBody)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            logger.info("OrderFailedEvent sent to SQS for Order ID= {}", agentCustomerAndOrderData.getOrderId());

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AgentAssignedFailedEvent event with message ID: {}. Error: {}", eventMessage.messageId(), e.getMessage(), e);
            throw new EventParsingException("Failed to parse AgentAssignedFailedEvent event: " + e.getMessage(), e);
        }
        return orderId;
    }

    private String processInventoryReservationFailed(Message eventMessage) throws EventParsingException {
        String orderId = null;
        try {
            final var inventoryAssignmentFailedEventDataEvent = objectMapper.readValue(
                    eventMessage.body(),
                    new TypeReference<Event<InventoryReservedFailedEventData>>() {
                    }
            );
            final var orderIdData = inventoryAssignmentFailedEventDataEvent.getData();
            orderId=orderIdData.getOrderId();

            // Update the order status to FAILED in the database
            orderRepository.findByOrderId(orderIdData.getOrderId())
                    .filter(order -> OrderStatus.ORDER_PENDING.equals(order.getOrderStatus())) // Ensure order is still in "PENDING" state
                    .ifPresent(order -> {
                        logger.info("Updating order status to FAILED for orderId: {}", order.getOrderId());
                        order.setOrderStatus(OrderStatus.ORDER_FAILED);
                        orderRepository.save(order);
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse InventoryReservedFailedEvent event with message ID: {}. Error: {}", eventMessage.messageId(), e.getMessage(), e);
            throw new EventParsingException("Failed to parse InventoryReservedFailedEvent event: " + e.getMessage(), e);
        }
        return orderId;
    }

    public String generateOrderId() {
        return UUID.randomUUID().toString();
    }


    /**
     * Creates and initializes a new Order entity with the provided customer and item details.
     *
     * @param customerId      The unique identifier of the customer placing the order
     * @param orderItemsDto   List of order items containing product details and quantities
     * @param orderId        Unique identifier for the order
     *
     * Process flow:
     * 1. Creates new Order instance and sets customer details
     * 2. Converts OrderItemDto list to OrderItem entities
     * 3. Associates OrderItems with the Order
     * 4. Sets initial order status as PENDING
     *
     * @return Order         Fully initialized Order entity
     * @logs                 Info level log when order is successfully generated
     */
    private Order getOrder(Long customerId, List<OrderItemDto> orderItemsDto, String orderId) {

        final var order = new Order();
        order.setCustomerId(customerId);
        // Set agent ID- Null for now as no agent is assigned
        order.setDeliveryAgentId(null);
        // order.setOrderDate(LocalDateTime.now());  // Set the current date

        // Step 2: Create and add OrderItems
        final var orderItems=orderItemsDto.stream().map(orderItemDto -> {
           final var orderItem = new OrderItem();
            orderItem.setItemName(orderItemDto.getItemName());
            orderItem.setQuantity(orderItemDto.getQuantity());
            orderItem.setOrder(order);
            return orderItem;
        }).toList();

        // Set the orderItems list in the Order entity
        order.setOrderItems(orderItems);
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.ORDER_PENDING);
        return order;
    }

    /**
     * Cancels an order and publishes a cancellation event to SNS.
     *
     * @param orderId The unique identifier of the order to cancel
     * @throws OrderNotFoundException if the order doesn't exist
     */
    public void cancelOrder(String orderId) throws OrderNotFoundException {
        logger.info("Attempting to cancel order: orderId={}", orderId);
        final var orderOptional = orderRepository.findByOrderId(orderId);
        orderOptional
                .ifPresentOrElse(
                        order -> {
                            try {
                                logger.info("Found order to cancel: orderId={}, status={}",
                                        orderId,
                                        order.getOrderStatus());
                                logger.info("Pushing to Order cancelled Topic = {}", orderCancelledTopicArn);
                                publishOrderCancelledEvent(order);
                            } catch (JsonProcessingException e) {
                                logger.error("Failed to publish order cancellation: orderId={}, error={}",
                                        orderId,
                                        e.getMessage(),
                                        e);
                                throw new RuntimeException(e);
                            }
                        },
                        () -> {
                            logger.warn("Cannot cancel order - not found: orderId={}", orderId);
                            throw new OrderNotFoundException("Order with ID " + orderId + " not found");
                        }
                );
    }

    /**
     * Publishes an order cancellation event to SNS topic.
     * This method transforms order details into a cancellation event and publishes it to the configured SNS topic.
     *
     * @param order The order entity containing details to be published in the cancellation event
     * @throws JsonProcessingException If there's an error in serializing the event to JSON
     */
    public void publishOrderCancelledEvent(Order order) throws JsonProcessingException {
        final var orderItems = order.getOrderItems();
        final var orderItemsDto = orderItems.stream()
                .map(orderItem -> new OrderItemDto(orderItem.getItemName(), orderItem.getQuantity()))
                .toList();
        final var orderCancelledEventData = new OrderCancelledEventData(
                order.getCustomerId(),
                orderItemsDto,
                order.getOrderId(),
                order.getDeliveryAgentId()
        );
        final var orderCancelEvent = new OrderCancelledEvent(orderCancelledEventData);
        final var cancelDataMessageBody = objectMapper.writeValueAsString(orderCancelEvent);

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(orderCancelledTopicArn)
                .message(cancelDataMessageBody)
                .build();

        snsClient.publish(publishRequest);
    }

//    public void publishOrderCancelledEvent(Order order) throws JsonProcessingException {
//        final var orderItems=order.getOrderItems();
//        final var orderItemsDto=orderItems.stream().map(orderItem -> new OrderItemDto(orderItem.getItemName(),orderItem.getQuantity())).toList();
//        final var orderCancelledEventData=new OrderCancelledEventData(order.getCustomerId(),orderItemsDto,order.getOrderId(),order.getDeliveryAgentId());
//        final var orderCancelEvent = new OrderCancelledEvent(orderCancelledEventData);
//        final var cancelDataMessageBody = objectMapper.writeValueAsString(orderCancelEvent);
//
//        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
//                .queueUrl(orderCancelledQueueUrl)
//                .messageBody(cancelDataMessageBody)
//                .build();
//
//        sqsClient.sendMessage(sendMsgRequest);
//    }

    /**
     * Creates an OutboxEvent object containing the provided OrderPlacedEvent.
     *
     * @param orderPlacedEvent  Event object containing order placement details
     *
     * @return OutboxEvent object encapsulating the OrderPlacedEvent
     */
    private OutboxEvent createOutBoxEvent(OrderPlacedEvent orderPlacedEvent) {
        return new OutboxEvent("OrderPlacedEvent", orderPlacedEvent);
    }

    /**
     * Creates an OrderPlacedEvent with the provided order details.
     *
     * @param customerId     The unique identifier of the customer who placed the order
     * @param orderItemDtos  List of items in the order with their details
     * @param orderId       Unique identifier for the order being placed
     *
     * @return OrderPlacedEvent  Event object containing order placement details
     */
    private OrderPlacedEvent getOrderPlacedEvent(Long customerId, List<OrderItemDto> orderItemDtos, String orderId) {
        final var orderPlacedEventData = new OrderPlacedEventData(customerId, orderItemDtos, orderId);
        return new OrderPlacedEvent(orderPlacedEventData);
    }
}
