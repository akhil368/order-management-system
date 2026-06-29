package com.oms.inventoryservice.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
