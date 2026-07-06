# Config Server (port 8888)

Centralized configuration for every service. Part of the
[Order Management System](../README.md). **Start this first** — the other services fetch their
config from it at boot.

## Responsibility
One place to manage shared and per-service configuration, so a config change doesn't mean editing
each service. Exposes the standard endpoint `/{application}/{profile}`.

## What it serves (native backend, from `src/main/resources/config-repo/`)
- `application.yml` — shared defaults inherited by all services (Eureka URL, actuator, logging)
- `order-service.yml` — order-service datasource, Kafka producer, topics, circuit-breaker settings
- `inventory-service.yml` — inventory datasource, Kafka consumer settings
- `api-gateway.yml` — gateway routes, rate-limiter config

## How it works
Clients declare `spring.config.import: configserver:http://localhost:8888` and identify themselves by
`spring.application.name`; the server returns that service's merged config as properties. The backend
is a **server-side** concern — switching native → Git → JDBC → Vault never changes the clients.

## Tech
Spring Cloud Config Server, `native` profile (serves from the classpath `config-repo/`).

## Run
```bash
mvn spring-boot:run   # start FIRST
curl http://localhost:8888/order-service/default   # verify it serves config
```
