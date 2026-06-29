package com.oms.inventoryservice.service;

import com.oms.inventoryservice.dto.StockResponse;
import com.oms.inventoryservice.entity.Inventory;
import com.oms.inventoryservice.exception.InsufficientStockException;
import com.oms.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repository;

    @Transactional(readOnly = true)
    public StockResponse check(String productId, int quantity) {
        int available = repository.findById(productId)
                .map(Inventory::getQuantity)
                .orElse(0);
        return new StockResponse(productId, available, available >= quantity);
    }

    @Transactional
    public void reserve(String productId, int quantity) {
        int rowsUpdated = repository.reserve(productId, quantity);
        if (rowsUpdated == 0) {
            throw new InsufficientStockException(
                    "Not enough stock for " + productId + " (requested " + quantity + ")");
        }
        log.info("Reserved {} of {}", quantity, productId);
    }
}
