package com.oms.orderservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

// -------------------------------------------------------
// REQUEST DTO — what the client sends to place an order
// -------------------------------------------------------
// Using a separate DTO (not the entity directly) is a best
// practice: it decouples your API contract from your DB schema.
// If you add a DB column later, your API doesn't break.
// -------------------------------------------------------

@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
}
