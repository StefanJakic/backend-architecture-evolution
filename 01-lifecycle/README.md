# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.4.0-SNAPSHOT - Durable Order Persistence**

Orders are now stored in PostgreSQL. Flyway owns schema evolution, while JPA is isolated inside the infrastructure layer.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0-SNAPSHOT - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.

## Current flow

```text
HTTP
  ↓
OrderController
  ↓
OrderService
  ├──→ OrderRepository
  │       ↓
  │  PostgresOrderRepository
  │       ↓
  │  Spring Data JPA
  │       ↓
  │  PostgreSQL
  │
  └──→ Order
       ↑
  lifecycle invariant
```

## Run locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
gradle bootRun
```

Run tests:

```bash
gradle test
```

Integration tests require Docker because they run against PostgreSQL through Testcontainers.

## Next pressure

The application is durable but not yet safe against two requests updating the same Order concurrently.

That is the purpose of **ORD-140 - Concurrent Transition Protection**.
