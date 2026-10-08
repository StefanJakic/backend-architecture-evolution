# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.5.0-SNAPSHOT - Concurrent Transition Protection**

Orders are durable in PostgreSQL and stale concurrent lifecycle updates are now rejected through optimistic locking.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.
- **0.5.0-SNAPSHOT - Concurrent Transition Protection**: transactions and optimistic locking reject stale writes.

## Current flow

```text
HTTP
  ↓
OrderController
  ↓
OrderService  @Transactional
  ├──→ OrderRepository
  │       ↓
  │  PostgresOrderRepository
  │       ↓
  │  OrderJpaEntity @Version
  │       ↓
  │  PostgreSQL
  │
  └──→ Order
       ↑
  lifecycle invariant
```

## Concurrent update behavior

Two requests may both read the same Order version, but they cannot both commit a write based on it.

```text
A loads v0 -> CONFIRM -> commit v1
B loads v0 -> CANCEL  -> optimistic lock conflict -> HTTP 409
```

The winner is determined by which database update commits first. The loser is not silently overwritten.

## Run locally with Podman

Start PostgreSQL:

```bash
podman compose up -d
```

Run the application:

```bash
gradle bootRun
```

Integration tests use Testcontainers and require a Docker-compatible container socket. Podman users may need to expose/configure the Podman socket for Testcontainers.

## Next pressure

Concurrency control identifies stale competing writes. It does not make network retries safe.

The next feature is **ORD-150 - Idempotent Order Commands**.
