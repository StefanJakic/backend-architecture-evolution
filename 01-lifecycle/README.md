# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.9.0-SNAPSHOT - Transactional Outbox**

Order domain events are now persisted durably in PostgreSQL in the same transaction as Order state and lifecycle audit history.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.
- **0.5.0 - Concurrent Transition Protection**: transactions and optimistic locking reject stale writes.
- **0.6.0 - Idempotent Order Creation**: persisted retry keys prevent duplicate Orders.
- **0.7.0 - Lifecycle Audit Trail**: committed lifecycle changes append durable local history.
- **0.8.0 - Domain Events**: Order records named business facts after valid lifecycle changes.
- **0.9.0-SNAPSHOT - Transactional Outbox**: domain events commit atomically with Order state and audit history.

## Current write flow

```text
HTTP command
    ↓
OrderService @Transactional
    ↓
Order transition
    ├── changes state
    └── records domain event
    ↓
persist Order
    ↓
release event
    ├── append audit
    └── insert outbox row
    ↓
COMMIT
```

For Order creation, idempotency completion is also part of the same transaction.

## Reliability improvement

Before:

```text
DB COMMIT ✅
process crash 💥
in-memory event lost ❌
```

Now:

```text
BEGIN
  Order state
  audit
  outbox event
COMMIT
        ↓
process may crash
        ↓
event still exists in PostgreSQL ✅
```

## What is intentionally missing

The outbox is durable storage, not a delivery mechanism.

There is no Kafka producer or relay in this version yet.

## Run locally with Podman

```bash
podman compose up -d
gradle bootRun
```

## Next pressure

Durable events now survive process crashes, but they still need reliable external delivery.

The next feature is **ORD-190 - Reliable Kafka Publication**.
