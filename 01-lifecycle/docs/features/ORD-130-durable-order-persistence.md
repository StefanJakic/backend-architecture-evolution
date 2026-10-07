# ORD-130 - Durable Order Persistence

## Problem

The HTTP API introduced in ORD-120 stores Orders only in process memory. Restarting the application loses every Order.

The application needs durable storage without changing the domain lifecycle or the existing application use cases.

## Design

```text
OrderService
    ↓
OrderRepository
    ↓
PostgresOrderRepository
    ↓
SpringDataOrderRepository
    ↓
OrderJpaEntity
    ↓
PostgreSQL
```

The application still depends only on `OrderRepository`.

The PostgreSQL adapter translates between:

- `Order` - domain behavior and lifecycle invariants
- `OrderJpaEntity` - relational persistence representation

## Reconstitution

A persisted Order is rebuilt with:

```java
Order.restore(id, status)
```

This is intentionally different from exposing a public `setStatus(...)`. Reconstitution restores already-valid persisted state; normal business state changes still go through `confirm()`, `ship()`, `complete()`, and `cancel()`.

## Schema ownership

Flyway owns schema creation and evolution.

Hibernate uses:

```text
ddl-auto: validate
```

so application startup fails if entity mapping and schema disagree, but Hibernate does not silently mutate production schema.

## Testing

Persistence and HTTP integration tests run against PostgreSQL through Testcontainers.

H2 is intentionally not used because SQL behavior, schema constraints, and database semantics should match the production database family.

## Deliberate limitation

Durability does **not** imply concurrency safety.

The current flow is still conceptually:

```text
request A: load CREATED
request B: load CREATED

A -> confirm -> save CONFIRMED
B -> cancel  -> save CANCELLED
```

Both writes can succeed because there is no version check yet.

That becomes ORD-140 - Concurrent Transition Protection.
