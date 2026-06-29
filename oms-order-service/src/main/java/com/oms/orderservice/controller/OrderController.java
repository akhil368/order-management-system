package com.oms.orderservice.controller;

import com.oms.orderservice.client.InventoryClient;
import com.oms.orderservice.dto.OrderResponse;
import com.oms.orderservice.dto.PlaceOrderRequest;
import com.oms.orderservice.dto.StockResponse;
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

    private final InventoryClient inventoryClient;

    // POST /api/v1/orders — place a new order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        log.info("Received place order request for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/orders/{orderId} — get order by id
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    // GET /api/v1/orders/customer/{customerId} — get all orders for a customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @GetMapping("/stock-check/{productId}")
    public ResponseEntity<StockResponse> checkStock(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(inventoryClient.checkStock(productId, quantity));
    }
}
