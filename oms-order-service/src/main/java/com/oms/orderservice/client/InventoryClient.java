package com.oms.orderservice.client;

import com.oms.orderservice.dto.StockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @GetMapping("/api/v1/inventory/{productId}")
    StockResponse checkStock(@PathVariable("productId") String productId,
                             @RequestParam("quantity") int quantity);
}
