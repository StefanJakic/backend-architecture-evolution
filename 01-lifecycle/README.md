# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.10.0-SNAPSHOT - Reliable Kafka Publication**

Durable outbox events are now claimed by a lease-based relay, published to Kafka, and marked as delivered after broker acknowledgement.

Delivery semantics are explicitly **at-least-once**.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: `OrderService`, repository port, and in-memory adapter introduce an application boundary.
- **0.3.0 - HTTP Order API**: Spring Boot and REST expose the existing use cases without moving lifecycle logic into controllers.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA adapter, and Testcontainers replace runtime in-memory storage.
- **0.5.0 - Concurrent Transition Protection**: transactions and optimistic locking reject stale writes.
- **0.6.0 - Idempotent Order Creation**: persisted retry keys prevent duplicate Orders.
- **0.7.0 - Lifecycle Audit Trail**: committed lifecycle changes append durable local history.
- **0.8.0 - Domain Events**: Order records named business facts after valid lifecycle changes.
- **0.9.0 - Transactional Outbox**: domain events commit atomically with Order state and audit history.
- **0.10.0-SNAPSHOT - Reliable Kafka Publication**: a lease-based relay publishes durable outbox events with at-least-once semantics.

## End-to-end event flow

~~~text
HTTP command
    ↓
OrderService @Transactional
    ↓
Order transition
    ↓
Order + audit + outbox
    ↓
COMMIT

outbox PENDING
    ↓
relay claims batch
    ↓
IN_PROGRESS
    ↓
Kafka publish
    ↓
broker ack
    ↓
PUBLISHED
~~~

Kafka records use `orderId` as their key and include `eventId` and `eventType` headers.

## Failure semantics

If Kafka publication fails:

~~~text
IN_PROGRESS → PENDING → retry
~~~

If a relay crashes after claiming work:

~~~text
IN_PROGRESS → lease expires → claimable again
~~~

If Kafka acknowledges but PostgreSQL cannot record PUBLISHED:

~~~text
Kafka has event
outbox retries later
→ duplicate delivery is possible
~~~

Consumers must therefore be idempotent by `eventId`.

## Run locally with Podman

~~~bash
podman compose up -d
gradle bootRun
~~~

The local compose environment starts PostgreSQL and Apache Kafka.

## Next pressure

The service is now durable, concurrency-safe, retry-safe, auditable, and capable of reliable at-least-once event delivery.

The next feature is **ORD-200 - Operability / Production Baseline**.
