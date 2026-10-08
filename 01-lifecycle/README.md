# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.6.0-SNAPSHOT - Idempotent Order Creation**

Order creation is now safe to retry with an `Idempotency-Key`. Sequential and concurrent duplicate requests return the same Order instead of creating another one.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.
- **0.5.0 - Concurrent Transition Protection**: transactions and optimistic locking reject stale writes.
- **0.6.0-SNAPSHOT - Idempotent Order Creation**: persisted retry keys prevent duplicate Orders.

## Create an Order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Idempotency-Key: create-order-123"
```

Retry the exact same logical request with the same key and the API returns the same Order id.

## Current create flow

```text
HTTP Idempotency-Key
        ↓
    OrderService @Transactional
        ↓
find / claim idempotency key
        ↓
      create Order
        ↓
   persist Order
        ↓
complete idempotency record
        ↓
       COMMIT
```

PostgreSQL owns duplicate-key arbitration, so the behavior also works when requests hit different application instances.

## Run locally with Podman

```bash
podman compose up -d
gradle bootRun
```

## Next pressure

We can now explain **what the current state is** and safely create/retry an Order, but we still do not retain a business history of how the Order reached that state.

The next feature is **ORD-160 - Lifecycle Audit Trail**.
