# Order Service (port 8081)

Accepts customer orders, persists them to its own MySQL database (`order_db`), and publishes an
`OrderPlaced` event to Kafka. Part of the [Order Management System](../README.md).

## Responsibility
Owns the **order** side of checkout: capturing what the customer wants (order record, customer,
price) and announcing it. It does **not** call inventory directly for the reserve — it publishes an
event and moves on, so a slow/down inventory-service can't block the checkout.

## What it does
- `POST /api/v1/orders` — place an order → saves to `order_db`, publishes `OrderPlaced` to Kafka → `201`
- `GET  /api/v1/orders/{orderId}` — fetch an order
- `GET  /api/v1/orders/customer/{customerId}` — orders for a customer
- `GET  /api/v1/orders/stock-check/{productId}?quantity=n` — checks inventory via Feign, wrapped in a
  **Resilience4j circuit breaker** (returns a degraded fallback if inventory-service is down, not a 500)

## Key pieces
- `OrderService` / `OrderController` — order logic + REST API
- Kafka **producer** + `KafkaTopicConfig` — publishes `OrderPlaced`
- `InventoryClient` (Feign) + `InventoryClientService` (`@CircuitBreaker`) — resilient inventory call
- `GlobalExceptionHandler` — consistent error responses

## Config & tech
Spring Boot 3.2.5 (Java 17), Spring Cloud 2023.0.1, JPA/MySQL (`order_db`), Kafka producer, Eureka
client, Config client (fetches config from the Config Server at :8888). Circuit-breaker settings live
in the config server's `config-repo/order-service.yml`.

## Run / test
```bash
mvn spring-boot:run     # needs config-server + eureka up first, and infra via docker-compose
mvn verify              # integration test: persists to real MySQL + publishes to real Kafka (Testcontainers)
```
