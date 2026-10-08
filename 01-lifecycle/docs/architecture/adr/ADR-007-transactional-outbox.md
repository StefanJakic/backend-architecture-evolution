# ADR-007 - Persist domain events with a transactional outbox

## Status

Accepted

## Context

After-commit in-process event publication still allows a committed business change to lose its event if the process terminates before publication.

Publishing to Kafka directly inside the application transaction would introduce a dual-write problem because PostgreSQL and Kafka do not share the same local transaction boundary.

## Decision

Persist released Order domain events to a PostgreSQL outbox table in the same transaction as Order state and audit history.

Remove in-process after-commit publication from the write path.

Defer transport delivery to a separate relay.

## Consequences

### Positive

- Order state and its domain event commit atomically.
- Process crashes after commit do not erase the event.
- Invalid transitions create no outbox rows.
- Optimistic-lock losers roll back outbox rows.
- Idempotent create retries do not duplicate OrderCreated rows.
- Kafka is kept outside the business transaction.

### Negative

- Events are durable but not yet delivered.
- The outbox table will grow until a relay and retention policy exist.
- Event payload serialization becomes part of the persistence contract.
- Delivery will be at-least-once unless a stronger protocol is introduced.

## Alternatives considered

### Keep after-commit in-process publication

Rejected because process memory is not durable.

### Write PostgreSQL and Kafka directly from OrderService

Rejected because success in one system does not atomically guarantee success in the other.

### Distributed transaction / 2PC

Rejected because it adds substantial operational complexity and is not justified for this service.

### Use audit rows as the message source

Rejected because audit history and integration-event delivery have different contracts and lifecycle concerns.
