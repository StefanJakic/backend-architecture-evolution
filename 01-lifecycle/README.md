# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.3.0-SNAPSHOT - HTTP Order API**

Spring Boot is now the application runtime and HTTP delivery mechanism. The domain and application layers remain responsible for business behavior.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0-SNAPSHOT - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.

## Current flow

```text
HTTP
  ↓
OrderController
  ↓
OrderService
  ├──→ OrderRepository
  │       ↓
  │  InMemoryOrderRepository
  │
  └──→ Order
       ↑
  lifecycle invariant
```

## Run

This version uses Spring Boot 4.1.1 and Java 21.

```bash
gradle bootRun
```

The next evolution replaces in-memory storage with durable PostgreSQL persistence.
