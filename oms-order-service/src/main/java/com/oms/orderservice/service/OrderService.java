package com.oms.orderservice.service;

import com.oms.orderservice.dto.OrderResponse;
import com.oms.orderservice.dto.PlaceOrderRequest;
import com.oms.orderservice.event.OrderPlacedEvent;
import com.oms.orderservice.exception.OrderNotFoundException;
import com.oms.orderservice.model.Order;
import com.oms.orderservice.model.OrderStatus;
import com.oms.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-placed}")
    private String orderPlacedTopic;

    // -------------------------------------------------------
    // @Transactional ensures: if DB save succeeds but Kafka
    // publish fails, the DB save is rolled back.
    // This prevents inconsistency between DB and Kafka.
    // (Interview tip: mention this pattern — it shows maturity)
    // -------------------------------------------------------
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        log.info("Placing order for customer: {}, product: {}",
                request.getCustomerId(), request.getProductId());

        // 1. Save order to database
        Order order = Order.builder()
                .productId(request.getProductId())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved to DB with id: {}", savedOrder.getId());

        // 2. Publish event to Kafka
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .productId(savedOrder.getProductId())
                .customerId(savedOrder.getCustomerId())
                .customerEmail(savedOrder.getCustomerEmail())
                .quantity(savedOrder.getQuantity())
                .price(savedOrder.getPrice())
                .placedAt(LocalDateTime.now())
                .build();

        publishOrderPlacedEvent(event);

        // 3. Return response
        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // -------------------------------------------------------
    // Private helper: publish event to Kafka
    // Uses orderId as the key so all events for the same order
    // go to the same partition (preserving order per orderId)
    // -------------------------------------------------------
    private void publishOrderPlacedEvent(OrderPlacedEvent event) {
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future =
                kafkaTemplate.send(orderPlacedTopic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("OrderPlacedEvent published successfully. " +
                                "OrderId: {}, Topic: {}, Partition: {}, Offset: {}",
                        event.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish OrderPlacedEvent for orderId: {}",
                        event.getOrderId(), ex);
                // In production: add to a retry queue or outbox table
            }
        });
    }

    // -------------------------------------------------------
    // Maps entity -> response DTO
    // Never return the entity directly from controller —
    // always map to DTO (interview best practice)
    // -------------------------------------------------------
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .productId(order.getProductId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
