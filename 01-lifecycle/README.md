# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.2.0-SNAPSHOT - Application Use Cases**

The code is still framework-free. The domain owns lifecycle invariants, while the application layer now coordinates use cases through a repository port.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0-SNAPSHOT - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.

## Current flow

```text
LifecycleDemo (composition root)
        ↓
    OrderService
        ↓
  OrderRepository
        ↓
InMemoryOrderRepository

OrderService
        ↓
      Order
  (owns lifecycle rules)
```

Spring Boot, HTTP, a real database, concurrency control, messaging, and operational concerns are intentionally deferred until a requirement justifies each one.
