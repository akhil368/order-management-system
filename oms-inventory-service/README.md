# Inventory Service (port 8082)

Consumes `OrderPlaced` events from Kafka and reserves stock in its own MySQL database
(`inventory_db`). Owns the **stock** side of checkout. Part of the
[Order Management System](../README.md).

## Responsibility
Track how much stock exists and decrement it **safely**. This is where the two hardest guarantees
live: no overselling, and no message left silently unprocessed.

## What it does
- Consumes `OrderPlaced` events and **reserves stock atomically**
- `GET /api/v1/inventory/{productId}?quantity=n` — check availability

## Key pieces
- `InventoryEventConsumer` — `@KafkaListener` that reserves stock per event
- `InventoryService` / `InventoryRepository` — the **atomic conditional reserve**:
  `UPDATE Inventory SET quantity = quantity - :qty WHERE productId = :id AND quantity >= :qty`.
  If it matches 0 rows there wasn't enough stock → `InsufficientStockException`. This is what makes
  overselling impossible even under concurrent orders.
- `KafkaConsumerConfig` — retry with exponential backoff, then route un-processable events to the
  **dead-letter topic** `order-placed-events.DLT`
- `DeadLetterConsumer` — reads the DLT (for logging/inspection)
- `data.sql` — seeds sample stock (PROD-1=100, PROD-2=50, PROD-3=10)

## Failure handling (the interesting part)
Transient errors are retried; permanent errors (e.g. malformed event, impossible quantity) are sent
straight to the DLT so one poison message can't block the queue or disappear.

## Config & tech
Spring Boot 3.2.5 (Java 17), JPA/MySQL (`inventory_db`), Kafka consumer + DLT, Eureka client, Config
client (:8888).

## Run / test
```bash
mvn spring-boot:run
mvn verify   # Testcontainers: reserve/oversell, event consumed, and permanent-failure → DLT
```
