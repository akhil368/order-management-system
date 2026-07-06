# API Gateway (port 8080)

The single entry point for the system. Routes external requests to the right service and applies
cross-cutting concerns. Part of the [Order Management System](../README.md).

## Responsibility
One front door instead of exposing every service. Clients only ever talk to `:8080`.

## What it does
- Routes `/api/v1/orders/**`  → `order-service` (via `lb://`, resolved through Eureka)
- Routes `/api/v1/inventory/**` → `inventory-service`
- **Redis-backed rate limiting** (`RequestRateLimiter`) — returns `429` when a client bursts past the limit
- `LoggingGlobalFilter` — logs every request in/out

## Key pieces
- `RateLimiterConfig` — key resolver (per client IP) for the rate limiter
- `LoggingGlobalFilter` — a Spring Cloud Gateway `GlobalFilter`
- Routes are defined in config (served by the Config Server: `config-repo/api-gateway.yml`)

## Tech
Spring Cloud Gateway (reactive / WebFlux — no `spring-boot-starter-web`), Redis, Eureka client,
Config client (:8888). Load-balanced routing uses Eureka service names, so no host/port is hardcoded.

## Run
```bash
mvn spring-boot:run   # start LAST, after config-server, eureka, and the two services
```
