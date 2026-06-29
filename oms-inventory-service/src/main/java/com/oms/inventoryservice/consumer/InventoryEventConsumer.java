package com.oms.inventoryservice.consumer;

import com.oms.inventoryservice.event.OrderPlacedEvent;
import com.oms.inventoryservice.exception.InsufficientStockException;
import com.oms.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-placed-events", groupId = "inventory-service-group")
    public void handleOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Inventory: received event. OrderId: {}, Partition: {}, Offset: {}",
                event.getOrderId(), partition, offset);

        try {
            processInventory(event);
        } catch (InsufficientStockException e) {
            log.error("Insufficient stock for product: {}. Sending to DLT.", event.getProductId());
            throw new IllegalArgumentException(e.getMessage());   // permanent -> not retried -> DLT
        } catch (Exception e) {
            log.error("Transient error for orderId: {}. Will retry.", event.getOrderId(), e);
            throw e;                                              // transient -> retried with backoff
        }
    }

    private void processInventory(OrderPlacedEvent event) {
        // Day-3 test hook: simulate a transient datastore outage -> should be retried
        if ("PROD-ERROR".equals(event.getProductId())) {
            throw new RuntimeException("DB temporarily unavailable - will retry");
        }
        // Day-3 test hook: clearly invalid -> permanent -> straight to DLT
        if (event.getQuantity() > 1000) {
            throw new IllegalArgumentException("Quantity exceeds maximum allowed limit");
        }
        // Real work: atomic reservation against the DB (may throw InsufficientStockException)
        inventoryService.reserve(event.getProductId(), event.getQuantity());
        log.info("Stock reserved for orderId: {} (product {})", event.getOrderId(), event.getProductId());
    }
}
