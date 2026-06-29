# OMS Order Service

Part of the **Order Management System** — an event-driven microservices architecture built with Spring Boot 3, Java 21, Apache Kafka, PostgreSQL, Redis, Docker, and Azure.

## Architecture Overview

```
Client
  │
  ▼
API Gateway (port 8080)
  │
  ▼
Order Service (port 8081)  ──► PostgreSQL (order_db)
  │
  │  publishes OrderPlacedEvent
  ▼
Apache Kafka (order-placed-events topic)
  │
  ├──► Inventory Service (reduces stock)
  └──► Notification Service (sends email)
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka |
| Database | PostgreSQL 16 |
| Caching | Redis 7 |
| Build | Maven |
| Container | Docker |
| Cloud | Azure (App Service / AKS) |
| CI/CD | GitHub Actions |

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- Docker Desktop

### Step 1: Start infrastructure
```bash
docker-compose up -d
```
This starts: Kafka, Zookeeper, PostgreSQL, Redis, Kafka UI

### Step 2: Run the service
```bash
mvn spring-boot:run
```

### Step 3: Test the API
```bash
# Place an order
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-001",
    "customerId": "CUST-001",
    "customerEmail": "test@example.com",
    "quantity": 2,
    "price": 499.99
  }'

# Get order by ID
curl http://localhost:8081/api/v1/orders/{orderId}

# Get orders by customer
curl http://localhost:8081/api/v1/orders/customer/CUST-001
```

### Step 4: View Kafka events
Open Kafka UI: http://localhost:8090
Navigate to Topics → order-placed-events → Messages

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/orders` | Place a new order |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders/customer/{id}` | Get all orders by customer |

## Project Structure

```
src/main/java/com/oms/orderservice/
├── controller/     # REST endpoints
├── service/        # Business logic + Kafka publishing
├── repository/     # JPA repositories
├── model/          # JPA entities
├── dto/            # Request/Response objects
├── event/          # Kafka event classes
├── config/         # Kafka topic config
└── exception/      # Custom exceptions + global handler
```

## Related Services
- [oms-inventory-service](https://github.com/yourusername/oms-inventory-service)
- [oms-notification-service](https://github.com/yourusername/oms-notification-service)
- [oms-api-gateway](https://github.com/yourusername/oms-api-gateway)
- [oms-config-server](https://github.com/yourusername/oms-config-server)
- [oms-infrastructure](https://github.com/yourusername/oms-infrastructure)
