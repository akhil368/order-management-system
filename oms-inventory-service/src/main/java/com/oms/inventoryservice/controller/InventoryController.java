package com.oms.inventoryservice.controller;

import com.oms.inventoryservice.dto.StockResponse;
import com.oms.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public StockResponse check(@PathVariable String productId,
                               @RequestParam(defaultValue = "1") int quantity) {
        return inventoryService.check(productId, quantity);
    }
}
