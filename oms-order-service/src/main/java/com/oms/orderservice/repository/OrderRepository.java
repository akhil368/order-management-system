package com.oms.orderservice.repository;

import com.oms.orderservice.model.Order;
import com.oms.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // Find all orders for a specific customer
    List<Order> findByCustomerId(String customerId);

    // Find all orders with a specific status
    List<Order> findByStatus(OrderStatus status);

    // Find all orders for a specific product
    List<Order> findByProductId(String productId);
}
