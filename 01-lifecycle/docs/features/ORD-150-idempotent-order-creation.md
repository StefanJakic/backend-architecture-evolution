# ORD-150 - Idempotent Order Creation

## Problem

A client cannot know whether a timed-out request failed before reaching the server or succeeded and lost its response.

Without idempotency:

```text
POST /orders
  -> Order A created
  -> response lost

client retries

POST /orders
  -> Order B created
```

One logical operation produced two Orders.

## API contract

Order creation now requires:

```http
POST /orders
Idempotency-Key: 87d91f2f-...
```

The client must reuse the same key when retrying the same logical create operation.

## Application flow

```text
find completed key?
  yes -> return stored Order id
  no
   ↓
try to claim key
   ↓
claimed?
  yes -> create Order
         -> complete idempotency record
         -> commit
  no  -> read the result committed by the competing request
```

## Why INSERT ... ON CONFLICT?

The database primary key is the concurrency boundary.

Two app instances may execute this at the same time:

```sql
INSERT INTO order_creation_idempotency (idempotency_key, status)
VALUES (?, 'IN_PROGRESS')
ON CONFLICT (idempotency_key) DO NOTHING;
```

Only one transaction can claim the key.

With PostgreSQL's default READ COMMITTED isolation, a competing insert waits for the conflicting transaction to finish. If the first transaction commits, the loser receives zero inserted rows and can read the completed result in its next statement.

If the first transaction rolls back, its claim disappears and the competing request can claim the key instead.

## Atomicity

The existing `@Transactional` boundary on `OrderService` now covers:

```text
claim idempotency key
  -> insert Order
  -> mark key COMPLETED with order_id
  -> commit
```

Therefore we do not commit an idempotency result without its Order, and a failed transaction does not permanently consume the key.

## Why PostgreSQL instead of Redis?

Both the Order and the idempotency record currently live in PostgreSQL.

Using one database gives us one local transaction and one consistency boundary.

Redis may become useful later for throughput, expiration, or cross-service coordination, but adding it now would introduce a distributed consistency problem before we have a requirement for it.

## Why no request fingerprint yet?

`POST /orders` currently has no request body.

There is therefore no meaningful case where the same idempotency key is reused with different create payloads.

If Order creation later accepts business input, the idempotency record should store a request fingerprint and reject:

```text
same key + different request
```

instead of replaying the first result.

## Scope

This feature protects only Order creation.

Lifecycle commands are not generalized behind a reusable idempotency framework. They still rely on domain transition validation and optimistic locking.

That keeps the implementation tied to a real requirement rather than creating infrastructure for hypothetical future commands.
