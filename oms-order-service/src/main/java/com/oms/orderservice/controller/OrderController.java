package com.oms.orderservice.controller;

import com.oms.orderservice.dto.OrderResponse;
import com.oms.orderservice.dto.PlaceOrderRequest;
import com.oms.orderservice.dto.StockResponse;
import com.oms.orderservice.service.InventoryClientService;
import com.oms.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final InventoryClientService inventoryClientService;   // Day 7: circuit-breaker-protected

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    /**
     * Calls inventory-service by logical name (Eureka + Feign), now wrapped in a
     * Resilience4j circuit breaker so it degrades gracefully when inventory is down.
     */
    @GetMapping("/stock-check/{productId}")
    public ResponseEntity<StockResponse> checkStock(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(inventoryClientService.checkStock(productId, quantity));
    }
}