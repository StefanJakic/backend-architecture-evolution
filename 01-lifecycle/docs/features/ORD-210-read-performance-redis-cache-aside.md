# ORD-210 - Read Performance Optimization with Redis Cache-Aside

## Problem

The Order API already reads current status correctly from PostgreSQL:

~~~text
GET /orders/{orderId}
  ↓
OrderService.statusOf
  ↓
OrderRepository.findById
  ↓
PostgreSQL
~~~

The lookup is by the Order primary key, so the database path is already indexed.

Redis is not introduced to fix correctness.

It is introduced only to reduce repeated read latency and database load when the same current status is requested frequently.

## Decision

Cache only the current Order status.

Do not cache:

- audit history
- idempotency state
- optimistic-lock version
- outbox rows
- lifecycle transition decisions

PostgreSQL remains the source of truth.

## Read path

~~~text
statusOf(orderId)
  ↓
Redis

hit
  → return status

miss
  → PostgreSQL
  → put status in Redis with TTL
  → return status

Redis error
  → PostgreSQL
  → best-effort cache write
  → return status
~~~

A cache hit does not open the OrderService database transaction.

`statusOf()` uses transaction propagation `NOT_SUPPORTED`.

On a cache miss, the Spring Data repository still manages the database read transaction around `findById`.

## Write path

Lifecycle writes continue to use PostgreSQL only:

~~~text
BEGIN
  load Order
  transition
  save Order
  audit
  outbox
  request cache invalidation
COMMIT
  ↓
invalidate Redis status
~~~

The Redis adapter defers invalidation until `afterCommit` when called inside an active Spring transaction.

This avoids deleting a good cache entry for a database transaction that later rolls back.

## Why invalidate instead of update?

Updating Redis before the PostgreSQL commit can expose a status that never becomes durable if the transaction rolls back.

Invalidation is safer:

~~~text
PostgreSQL = source of truth
Redis = disposable copy
~~~

After invalidation, the next read reloads the committed value from PostgreSQL.

## Failure semantics

Redis is optional acceleration.

If Redis is unavailable:

~~~text
cache read fails
  ↓
record metric / log warning
  ↓
read PostgreSQL
  ↓
return correct result
~~~

The Order API remains correct.

Cache write or invalidation failure also does not fail the business transaction.

## TTL

Status entries have a finite TTL, default:

~~~text
5 minutes
~~~

TTL is not the primary consistency mechanism.

Write invalidation is.

TTL is a safety bound if invalidation is missed or Redis contains an old value.

## Consistency trade-off

There is still a very small eventual-consistency window around commit and after-commit invalidation.

A concurrent reader can theoretically observe the previous cached value immediately around the database commit.

This skeleton accepts that trade-off because current status is a read optimization and PostgreSQL remains authoritative.

Systems requiring strict read-after-write consistency would need a stronger strategy.

## Metrics

~~~text
order.status.cache.requests{result=hit}
order.status.cache.requests{result=miss}

order.status.cache.errors{operation=read}
order.status.cache.errors{operation=write}
order.status.cache.errors{operation=invalidate}
~~~

These metrics answer whether the cache is useful and whether Redis is degrading.

## Why history is not cached

Lifecycle history is append-only and less likely to be the hot read path in this skeleton.

Caching it would introduce more invalidation/versioning behavior without a demonstrated problem.

## What this feature teaches

- database-first optimization
- cache-aside
- TTL
- invalidation
- fail-open cache behavior
- optional infrastructure dependencies
- stale-read trade-offs
- measuring hit rate instead of assuming cache value

## Next pressure

The application architecture is now complete for this skeleton.

The final step is deployment packaging:

- container image
- Kubernetes manifests
- AWS target architecture
- release `1.0.0`
