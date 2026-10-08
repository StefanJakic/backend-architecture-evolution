# ADR-008 - Publish outbox events to Kafka with at-least-once delivery

## Status

Accepted

## Context

Transactional outbox persistence guarantees that Order state and its domain event commit together.

It does not deliver the event to another system.

PostgreSQL and Kafka do not share the same local transaction, so the relay must choose explicit failure semantics.

## Decision

Use a polling outbox relay with:

- PostgreSQL PENDING / IN_PROGRESS / PUBLISHED state
- short claims using FOR UPDATE SKIP LOCKED
- claim leases for crash recovery
- Kafka orderId record keys
- broker acknowledgement before marking PUBLISHED
- retry after failed publication
- eventId as the downstream idempotency identity

Delivery is documented as **at-least-once**.

## Why not keep the database lock during Kafka I/O?

Holding the claim transaction open until Kafka replies would keep row locks and database connections occupied across an external network dependency.

Instead, the relay commits the claim quickly and uses a lease to recover abandoned work.

## Why not claim exactly-once?

There is an unavoidable failure window without a distributed atomic protocol:

~~~text
Kafka ack succeeds
PostgreSQL mark PUBLISHED fails
~~~

Retrying is necessary to avoid loss, and retrying can duplicate the Kafka record.

Kafka producer idempotence does not close this database/Kafka boundary.

## Consumer requirement

Consumers must be idempotent by `eventId`.

A typical consumer can keep a processed-event table with a unique event id and perform:

~~~text
claim event id
apply business effect
commit
~~~

inside its own local transaction.

## Consequences

### Positive

- durable events are eventually publishable after transient Kafka failures
- crashed relay workers do not permanently strand IN_PROGRESS rows
- multiple service replicas can share relay work
- per-Order Kafka keying preserves partition affinity
- duplicate delivery is explicit rather than hidden

### Negative

- consumers must handle duplicate event ids
- polling adds database traffic
- abandoned claims wait for lease expiry before recovery
- published outbox rows require future retention or archival policy

## Alternatives considered

### Direct Kafka publish from OrderService

Rejected because it recreates the PostgreSQL/Kafka dual-write problem.

### Hold a PostgreSQL transaction open until Kafka acknowledgement

Rejected because external I/O would extend database locks and connection usage.

### Claim exactly-once delivery

Rejected because the relay cannot atomically commit Kafka acknowledgement and PostgreSQL publication state.

### Distributed transaction / 2PC

Rejected because the operational complexity is not justified for this service.
