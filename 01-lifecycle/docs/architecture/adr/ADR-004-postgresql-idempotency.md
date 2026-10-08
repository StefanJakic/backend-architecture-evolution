# ADR-004 - Store Order creation idempotency in PostgreSQL

## Status

Accepted

## Context

Clients may retry `POST /orders` when a network timeout makes the original outcome unknown.

The system must prevent duplicate Orders across retries and across multiple application instances.

## Decision

Require an `Idempotency-Key` for Order creation and persist its state in PostgreSQL.

Use a primary-key uniqueness constraint and `INSERT ... ON CONFLICT DO NOTHING` to claim a key.

Keep the idempotency claim, Order insert, and completion update in the same application transaction.

## Consequences

### Positive

- Retries return the original Order id.
- Concurrent duplicate requests cannot both own the same key.
- The solution works across multiple application instances.
- Failed transactions release their uncommitted claim automatically.
- Order and idempotency state share one transaction boundary.

### Negative

- A duplicate request can wait for the first transaction to finish.
- Idempotency records add persistent storage that will eventually need retention/cleanup policy.
- The implementation currently relies on PostgreSQL READ COMMITTED behavior for the post-conflict replay flow.

## Alternatives considered

### JVM-local map or lock

Rejected because it fails across multiple application instances and loses state on restart.

### Redis

Not selected yet because Order state is already in PostgreSQL and one database transaction is simpler than coordinating PostgreSQL with Redis.

### Check-then-insert without a unique constraint

Rejected because two concurrent requests can both observe that the key is absent and create duplicate Orders.

### Generic idempotency framework for all commands

Deferred. Only Order creation currently has a concrete retry-duplication requirement.
