# ORD-180 - Transactional Outbox

## Problem

ORD-170 introduced explicit domain events and published them after the database transaction committed.

That removed one problem:

```text
listener reacts
transaction rolls back
```

but left another:

```text
database COMMIT succeeds
process crashes
event publication never happens
```

The Order state is durable while the event exists only in process memory.

## Decision

Persist every released Order domain event into `order_event_outbox` inside the same PostgreSQL transaction as:

- Order state
- lifecycle audit
- idempotency completion

The write flow is now:

```text
BEGIN

domain transition
persist Order
release domain event
append audit
insert outbox event

COMMIT
```

For creation, idempotency completion is part of the same transaction.

## Atomicity

The important guarantee is not merely that an outbox table exists.

The guarantee is:

```text
Order state + audit + outbox
```

commit together or roll back together.

If outbox persistence throws after the Order update and audit insert have already executed, Spring rolls the whole transaction back.

## Outbox row

Each row contains:

```text
event_id
order_id
event_type
payload
occurred_at
recorded_at
```

`event_id` comes from the domain event and is the stable identity of that fact.

`payload` stores the serialized event as JSONB.

## Why no published flag yet?

This feature solves event durability, not delivery.

There is currently no relay, so adding fields such as:

```text
published_at
attempt_count
last_error
locked_by
```

would model behavior that does not exist yet.

ORD-190 will add only the delivery state that its relay actually needs.

## Idempotency

A repeated successful `POST /orders` with the same Idempotency-Key returns before new Order creation.

Therefore it does not create another:

- Order
- CREATE audit entry
- OrderCreated outbox row

## Concurrency

If two lifecycle commands race on the same Order, optimistic locking allows one write to commit.

The losing transaction rolls back its audit and outbox work, so the durable history contains only the winning business fact.

## What the outbox does not solve

The outbox makes the event durable.

It does not yet deliver anything.

After commit we now have:

```text
PostgreSQL
  orders
  order_lifecycle_audit
  order_event_outbox
```

A separate relay is still required to read outbox rows and send them to Kafka.

## Next pressure

**ORD-190 - Reliable Kafka Publication**

Add a relay that publishes durable outbox records, handles retry-safe delivery, and marks successful publication.
