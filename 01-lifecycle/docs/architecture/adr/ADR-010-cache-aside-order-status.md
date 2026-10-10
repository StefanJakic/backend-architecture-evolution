# ADR-010 - Use Redis cache-aside only for current Order status

## Status

Accepted

## Context

Current Order status is read from PostgreSQL by primary key.

The lookup is already correct and indexed, but a read-heavy workload can still create unnecessary database traffic for repeatedly requested Orders.

Introducing Redis increases operational and consistency complexity, so its scope must remain narrow.

## Decision

Use Redis as an optional cache-aside layer for current Order status only.

PostgreSQL remains authoritative.

Cache behavior:

- read Redis first
- on miss, read PostgreSQL and cache the status
- use a finite TTL
- invalidate after successful lifecycle transaction commit
- fail open to PostgreSQL when Redis is unavailable

## Why status only?

Current status is:

- small
- frequently reusable
- cheap to serialize
- safe to reconstruct from PostgreSQL

Audit history and write-side coordination state have different consistency and lifecycle requirements and remain uncached.

## Why after-commit invalidation?

Deleting the cache entry before commit can create a race:

~~~text
writer deletes cache
reader misses cache
reader reads old uncommitted database state
reader caches old status
writer commits
~~~

Deferring invalidation until after commit reduces this stale-repopulation window and avoids cache changes for rolled-back transactions.

## Why not write-through?

Writing the new status into Redis before the database commit risks publishing state that later rolls back.

PostgreSQL owns correctness, so cache invalidation is preferred over trying to coordinate two writes.

## Why fail open?

Redis is an optimization.

A Redis outage must not become an Order API outage while PostgreSQL is healthy.

## Consequences

### Positive

- repeated status reads can avoid PostgreSQL queries
- Redis outages do not break correctness
- cache lifecycle is narrow and understandable
- hit/miss/error metrics show whether the optimization is paying off

### Negative

- a short stale-read window still exists around commit/invalidation
- Redis adds infrastructure and monitoring
- invalidation failures rely on TTL as a safety bound
- cache stampede protection is not implemented

## Alternatives considered

### Cache everything

Rejected because it expands invalidation and consistency complexity without evidence that every read path is hot.

### Redis as source of truth

Rejected because lifecycle transactions, optimistic locking, audit, and outbox correctness already belong to PostgreSQL.

### Write-through cache

Rejected because PostgreSQL and Redis do not share the same atomic transaction.

### No cache

Still a valid deployment choice. The cache can be disabled without changing application correctness.
