package com.oms.inventoryservice.repository;

import com.oms.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    /**
     * Atomic check-and-decrement in a single statement.
     * Returns 1 if reserved, 0 if not enough stock (or product missing).
     * No read-modify-write race -> cannot oversell; no row lock held.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty " +
           "WHERE i.productId = :productId AND i.quantity >= :qty")
    int reserve(@Param("productId") String productId, @Param("qty") int qty);
}
