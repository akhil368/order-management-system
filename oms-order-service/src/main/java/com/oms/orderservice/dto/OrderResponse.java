package com.oms.orderservice.dto;

import com.oms.orderservice.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String productId;
    private String customerId;
    private String customerEmail;
    private Integer quantity;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
