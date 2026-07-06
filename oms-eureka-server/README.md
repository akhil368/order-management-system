# Eureka Server (port 8761)

The service registry. Every other service registers here on startup, and the gateway + Feign use it
to find services by name instead of hardcoded host:port. Part of the
[Order Management System](../README.md).

## Responsibility
Service discovery. When order-service wants inventory-service, it asks Eureka for "inventory-service"
and gets a live address — so services can move, scale, or restart without anyone hardcoding URLs.

## What it does
- Hosts the Eureka dashboard at `http://localhost:8761`
- Accepts registrations from order-service, inventory-service, and api-gateway
- Provides load-balanced lookup used by the gateway (`lb://...`) and Feign clients

## Tech
Spring Cloud Netflix Eureka Server. It is standalone infrastructure — it does **not** register with
itself and is **not** a config client (its own config stays local).

## Run
```bash
mvn spring-boot:run   # start second, right after the config server
```
