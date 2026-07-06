# Order Management System (OMS) — Event-Driven Microservices

The backend for an e-commerce checkout: when a customer places an order, the system records the
order, reserves the stock, and does so **reliably even when parts of the system fail** — without
ever overselling inventory.

This project deliberately focuses on the hard parts real distributed systems face — failing
services, lost or malformed messages, and concurrent updates to the same stock — rather than a
happy-path CRUD demo.

---

## The problem it solves

In a real store, "place an order" and "manage stock" are two different jobs with different owners,
different data, and different load patterns (orders spike on a sale day; stock updates don't). If
you build them as one service they become tightly coupled — a slow inventory update can stall the
checkout, and one bug forces a redeploy of everything. Splitting them raises three hard questions
this project answers:

1. **How do two services stay consistent without blocking each other?** → they communicate through
   **Kafka events**, not direct calls. The order-service records the order and publishes an
   `OrderPlaced` event; the inventory-service consumes it and reserves stock on its own time. If
   inventory-service is briefly down, events wait in Kafka and are processed when it recovers —
   nothing is lost, and the checkout never blocks on it.
2. **What happens to a message that can't be processed?** (malformed event, unknown product) → after
   a few retries it is routed to a **dead-letter topic** instead of blocking the queue or vanishing
   silently. Good messages keep flowing; bad ones are quarantined for inspection.
3. **How do you avoid overselling the last item under concurrent orders?** → stock is reserved with
   a single **atomic conditional UPDATE** (`... SET quantity = quantity - :qty WHERE quantity >= :qty`).
   The database itself guarantees only one order can take the last unit.

Plus: a **circuit breaker** so a down dependency degrades gracefully instead of cascading, and
**integration tests against real Kafka + MySQL** so the failure handling is proven, not mocked.

---

## Architecture

```
                       ┌──────────────────┐
   client ──▶ API Gateway (8080) ─ routes ─┤
                       └──────────────────┘
                          │            │
              /api/v1/orders      /api/v1/inventory
                          ▼            ▼
                 Order Service     Inventory Service
                   (8081)             (8082)
                   │   │                 ▲   │
        order_db ◀─┘   │  OrderPlaced    │   └─▶ inventory_db
                       └──▶  Kafka  ──────┘
                              │  (retry ×N, then)
                              └──▶ order-placed-events.DLT

   Supporting: Config Server (8888) serves centralized config to all services;
               Eureka (8761) is the service registry used for discovery + gateway routing.
```

- **Order Service (8081)** — accepts orders, persists to `order_db`, publishes `OrderPlaced` events.
- **Inventory Service (8082)** — consumes events, reserves stock atomically in `inventory_db`,
  routes un-processable events to the dead-letter topic.
- **API Gateway (8080)** — single entry point; routes by path via Eureka, Redis-backed rate limiting.
- **Config Server (8888)** — centralized configuration for all services.
- **Eureka Server (8761)** — service registry for discovery and load-balanced routing.

Database-per-service: each service owns its own schema and no service reads another's tables.

---

## Tech stack

- Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1
- Apache Kafka (event backbone) with retry + dead-letter topic
- Spring Cloud: Netflix Eureka, Gateway, Config Server, OpenFeign, Resilience4j
- MySQL 8 (per-service databases), Redis (gateway rate limiting)
- Testcontainers + JUnit 5 (integration tests against real Kafka & MySQL)
- Docker + Docker Compose; GitHub Actions CI/CD (build → test → publish images to GHCR)

---

## Key design decisions (the "why")

| Decision | Why |
|---|---|
| Event-driven (Kafka) instead of direct calls | Decouples order & inventory; inventory being slow/down can't block or crash checkout. |
| Dead-letter topic | A poison message would otherwise block the queue or be lost; DLT quarantines it after retries. |
| Atomic conditional UPDATE for reserve | Prevents overselling under concurrency — the DB guarantees only one order wins the last unit. |
| Circuit breaker (Resilience4j) | Stops a failing dependency from cascading; fails fast with a fallback instead of hanging threads. |
| Database-per-service | Independent ownership, schema, and scaling; no shared-DB coupling. |
| Centralized config (Config Server) | One place for shared/per-service config; change without touching each service. |
| Testcontainers integration tests | Proves the Kafka/DLT and DB logic against real infrastructure, not mocks. |

---

## Running it locally

Prerequisites: Docker Desktop, JDK 17, Maven.

```bash
# 1) start infrastructure (Kafka, Zookeeper, MySQL, Redis, Kafka UI)
docker-compose up -d

# 2) start services IN THIS ORDER (each fetches config from the config server, then registers with Eureka)
#    config-server (8888) → eureka-server (8761) → order-service (8081) → inventory-service (8082) → api-gateway (8080)
```
Or run the whole system in containers:
```bash
docker compose -f docker-compose.full.yml up --build
```

### Try it (via the gateway)
```bash
# place an order  → persists to MySQL AND publishes an OrderPlaced event
curl -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -d '{
  "productId":"PROD-1","customerId":"CUST-1","customerEmail":"a@b.com","quantity":2,"price":499.00
}'

# check stock
curl "http://localhost:8080/api/v1/inventory/PROD-1?quantity=2"
```
Full test walkthrough (happy path, dead-letter demo, circuit breaker, rate limiting) is in
[`TESTING.md`](TESTING.md).

---

## Tests

```bash
cd oms-order-service     && mvn verify
cd oms-inventory-service && mvn verify
```
Integration tests spin up real MySQL + Kafka via Testcontainers (Docker required). They assert:
stock decrements correctly, oversell is impossible, an `OrderPlaced` event is consumed and reserves
stock, and a permanent-failure event lands on the dead-letter topic.

> Note: on some very new Docker Desktop builds the local Testcontainers run can hit a client/daemon
> API-version mismatch. These tests run reliably in CI (GitHub Actions), where the runner's Docker
> is compatible.

---

## CI/CD

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push/PR to `main`:
1. **build-and-test** — builds and tests all services on a fresh runner (Testcontainers tests run
   green here).
2. **build-and-push-images** — on `main`, after tests pass, builds each service image and pushes it
   to GitHub Container Registry (GHCR).

An Azure Container Apps `deploy` job can be added on top (gated on the image build) when a cloud
account is available.

---

## Repository layout

```
Order Management Systtem/
├── pom.xml                      # parent aggregator (multi-module Maven)
├── docker-compose.yml           # infra only (run services from IDE)
├── docker-compose.full.yml      # full stack in containers
├── oms-config-server/           # centralized config (8888)
├── oms-eureka-server/           # service registry (8761)
├── oms-order-service/           # orders REST + Kafka producer + circuit breaker (8081)
├── oms-inventory-service/       # Kafka consumer + DLT + stock reservation (8082)
└── oms-api-gateway/             # single entry point + rate limiting (8080)
```
