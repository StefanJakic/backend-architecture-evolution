# ORD-140 - Concurrent Transition Protection

## Problem

ORD-130 made lifecycle state durable, but two requests can still make decisions from the same old state.

Example:

```text
database: CREATED v0

request A loads CREATED v0
request B loads CREATED v0

A decides: confirm
B decides: cancel
```

Without concurrency control, whichever write happens last can silently overwrite the other.

## Decision

Use optimistic locking.

The `orders` table now contains a `version` column and `OrderJpaEntity` maps it with `@Version`.

Hibernate updates a row using the version it originally loaded. Conceptually:

```sql
UPDATE orders
SET status = ?, version = 1
WHERE id = ? AND version = 0;
```

If another transaction already changed the row to version 1, the stale update affects zero rows and is rejected.

## Why the transaction boundary matters

The lifecycle use case must be one unit:

```text
load
  -> domain transition
  -> save
  -> commit
```

If load and save were separate transactions, `save()` could reload the newest database version and accidentally hide the fact that the domain decision was made from stale state.

For that reason, `OrderService` now owns the transaction boundary.

## Why OrderService is no longer final

Spring applies `@Transactional` through a proxy. With our concrete class and no service interface, Spring uses a class-based proxy, which needs a non-final class/method.

We deliberately do not introduce an interface only to satisfy the framework. Removing `final` is the smaller trade-off here.

## Conflict translation

Infrastructure exception:

```text
OptimisticLockingFailureException
```

is translated into:

```text
ConcurrentOrderModificationException
```

and the HTTP layer returns:

```text
409 Conflict
ORDER_CONCURRENT_MODIFICATION
```

The API therefore does not expose Hibernate or Spring exception types.

## Test strategy

The concurrency integration test does not depend on timing sleeps.

It blocks two requests after both have loaded the same Order version, then releases them together:

```text
A loaded v0 ─┐
             ├─ release
B loaded v0 ─┘
```

Exactly one transition must commit and the stale competitor must fail.

## Why optimistic instead of pessimistic locking?

Expected contention on one Order is low.

Optimistic locking lets independent Orders proceed without acquiring a database lock up front and pays the conflict cost only when two writes actually race.

If contention became frequent, this assumption would need to be revisited.

## Next pressure

A client that times out may retry the same command.

Concurrency protection prevents stale overwrites, but it does not tell the server whether two identical requests represent one logical operation or two.

That becomes ORD-150 - Idempotent Order Commands.
