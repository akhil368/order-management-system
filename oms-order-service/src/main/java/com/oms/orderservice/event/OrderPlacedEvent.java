package com.oms.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// -------------------------------------------------------
// This is the event published to Kafka.
// Keep it flat and simple — no JPA annotations,
// no business logic. Just data.
// In a real company this would live in a shared
// 'oms-events' library imported by all services.
// -------------------------------------------------------

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String productId;
    private String customerId;
    private String customerEmail;
    private Integer quantity;
    private BigDecimal price;
    private LocalDateTime placedAt;
}
