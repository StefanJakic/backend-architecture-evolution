# ORD-190 - Reliable Kafka Publication

## Problem

ORD-180 made domain events durable by committing them to PostgreSQL with Order state.

The event now survives process crashes, but it still remains local:

~~~text
PostgreSQL outbox
    ↓
nothing delivers it externally
~~~

## Delivery lifecycle

Outbox rows now have explicit delivery state:

~~~text
PENDING
   ↓ claim
IN_PROGRESS
   ↓ broker ack
PUBLISHED
~~~

A failed Kafka send returns the row to PENDING.

An IN_PROGRESS row whose lease expires can be claimed again after a crashed worker disappears.

## Claiming work

A relay claims a batch with PostgreSQL:

~~~sql
FOR UPDATE SKIP LOCKED
~~~

The claim transaction is short:

~~~text
BEGIN
  select claimable rows
  mark IN_PROGRESS
COMMIT

Kafka network call happens afterwards
~~~

We deliberately do not hold database row locks while waiting for Kafka.

This also allows multiple application replicas to divide queue-like work without intentionally selecting the same unlocked row.

## Kafka record

The Kafka key is:

~~~text
orderId
~~~

so events belonging to one Order are partition-affine.

Headers include:

~~~text
eventId
eventType
~~~

The JSON payload also contains the domain event id.

## Broker acknowledgement

The relay waits for the Kafka send future to complete before marking the outbox row PUBLISHED.

Producer configuration uses:

~~~text
acks=all
enable.idempotence=true
~~~

Producer idempotence protects Kafka producer retries within the producer protocol.

It does not make PostgreSQL and Kafka one atomic system.

## Why delivery is at-least-once

Consider:

~~~text
Kafka publish succeeds
        ↓
broker acknowledgement succeeds
        ↓
mark PUBLISHED in PostgreSQL fails
~~~

The row is retried later.

Kafka can therefore receive the same event again.

That is correct **at-least-once** behavior.

Consumers must treat `eventId` as an idempotency key.

## Failure behavior

### Kafka unavailable

~~~text
IN_PROGRESS
  ↓ send fails
PENDING
~~~

The next relay run can retry it.

### Relay process crashes after claim

~~~text
IN_PROGRESS
  ↓ no worker remains
lease expires
  ↓
claimable again
~~~

### Kafka succeeds but database mark fails

~~~text
Kafka has event
PostgreSQL does not know publication completed
  ↓
retry
  ↓
possible duplicate
~~~

No claim of exactly-once delivery is made.

## Outbox delivery metadata

ORD-190 adds delivery fields only now that they have concrete behavior:

~~~text
status
claimed_at
published_at
attempt_count
last_error
~~~

## Local runtime

`compose.yaml` now starts:

~~~text
PostgreSQL
Kafka
~~~

The application publishes to:

~~~text
order.lifecycle.v1
~~~

by default.

## Next pressure

**ORD-200 - Operability / Production Baseline**

- health/readiness
- metrics
- relay backlog metrics
- publish failure metrics
- structured logs
- correlation/request ids
- tracing
