# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.7.0-SNAPSHOT - Lifecycle Audit Trail**

The service now stores append-only history for every committed Order lifecycle change and exposes it through a read-only history endpoint.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.
- **0.5.0 - Concurrent Transition Protection**: transactions and optimistic locking reject stale writes.
- **0.6.0 - Idempotent Order Creation**: persisted retry keys prevent duplicate Orders.
- **0.7.0-SNAPSHOT - Lifecycle Audit Trail**: successful lifecycle changes append durable history in the same transaction as Order state.

## Current write flow

```text
HTTP command
    ↓
OrderService @Transactional
    ↓
load Order
    ↓
domain transition
    ↓
persist current state
    ↓
append audit history
    ↓
COMMIT
```

A failed domain transition or optimistic-lock conflict rolls the whole transaction back, so history contains only committed lifecycle changes.

## Read current state

```bash
curl http://localhost:8080/orders/{orderId}
```

## Read lifecycle history

```bash
curl http://localhost:8080/orders/{orderId}/history
```

Example history:

```text
CREATE  null      -> CREATED
CONFIRM CREATED   -> CONFIRMED
SHIP    CONFIRMED -> SHIPPED
```

## Run locally with Podman

```bash
podman compose up -d
gradle bootRun
```

## Next pressure

Audit gives us durable local history, but other parts of a system still have no explicit business signal that an Order was confirmed, shipped, or cancelled.

The next feature is **ORD-170 - Domain Events**.
