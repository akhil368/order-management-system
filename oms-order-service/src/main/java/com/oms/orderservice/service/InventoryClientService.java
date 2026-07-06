package com.oms.orderservice.service;

import com.oms.orderservice.client.InventoryClient;
import com.oms.orderservice.dto.StockResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Day 7: wraps the inventory Feign call with a Resilience4j circuit breaker.
 * Must be a SEPARATE bean from the controller — @CircuitBreaker works via a Spring proxy,
 * and a self-call within the same bean would bypass the proxy (and the breaker).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryClientService {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "stockCheckFallback")
    public StockResponse checkStock(String productId, int quantity) {
        return inventoryClient.checkStock(productId, quantity);   // protected remote call
    }

    /**
     * Fallback: SAME parameters + a Throwable at the end.
     * Fail SAFE: if stock can't be confirmed, report it unavailable rather than 500 the caller.
     */
    public StockResponse stockCheckFallback(String productId, int quantity, Throwable t) {
        log.warn("inventory-service unavailable for {} - returning degraded response. Cause: {}",
                productId, t.toString());
        return new StockResponse(productId, 0, false);
    }
}
