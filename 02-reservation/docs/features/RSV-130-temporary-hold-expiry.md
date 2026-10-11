# RSV-130 - Temporary Hold and Expiry

## Problem

A real booking flow often cannot confirm a reservation immediately.

The system may need to hold the resource while the user completes payment or another confirmation step.

That creates a new time problem:

> How can a slot be blocked temporarily, then become available again if the user does not finish in time?

## States

~~~text
HELD
  -> CONFIRMED
  -> EXPIRED
~~~

More precisely:

~~~text
HELD --confirm before expiresAt--> CONFIRMED
HELD --expiresAt reached---------> EXPIRED
~~~

CONFIRMED does not expire.

EXPIRED remains stored but no longer blocks the resource.

## Why expiry is an explicit state

The database does not use current wall-clock time inside the exclusion constraint.

Instead, expiry is materialized:

~~~text
HELD + expiresAt <= now
    ↓
EXPIRED
~~~

This makes the active/inactive state explicit and testable.

## Partial exclusion constraint

RSV-120 blocked overlap for every reservation row.

RSV-130 changes that to block only active states:

~~~sql
EXCLUDE USING gist (
    resource_id WITH =,
    (tstzrange(starts_at, ends_at, '[)')) WITH &&
)
WHERE (status IN ('HELD', 'CONFIRMED'))
~~~

Therefore:

~~~text
HELD       -> blocks
CONFIRMED  -> blocks
EXPIRED    -> does not block
~~~

PostgreSQL exclusion constraints support a predicate, which internally creates a partial index.

## Lazy expiry

Before creating a new hold or confirmed reservation:

~~~text
expire stale HELD rows
    ↓
COMMIT expiry cleanup
    ↓
continue reserve / hold / confirm
~~~

Expiry runs in a separate `REQUIRES_NEW` transaction.

That boundary is intentional.

Without it, this flow would be possible:

~~~text
BEGIN
mark hold EXPIRED
confirm request is rejected
ROLLBACK
=> row becomes HELD again
~~~

Expiry is maintenance state that must survive failure of the command that happened to trigger cleanup.

The same expiry operation is exposed separately:

~~~text
expireExpiredHolds()
~~~

A production scheduler could invoke it periodically.

This skeleton deliberately does not add scheduling infrastructure because that is not the learning objective.

## Hold creation

~~~text
now
 ↓
expiresAt = now + holdDuration
 ↓
expire old holds
 ↓
advisory overlap SELECT
 ↓
INSERT HELD
 ↓
PostgreSQL exclusion constraint is final arbiter
~~~

## Confirmation

~~~text
expire old holds
 ↓
load hold
 ↓
if EXPIRED -> HoldExpiredException
 ↓
HELD -> CONFIRMED
~~~

Both HELD and CONFIRMED are inside the same exclusion constraint.

The slot therefore never becomes temporarily free during confirmation.

## Why not delete expired rows?

Deletion would release capacity, but it would lose useful historical information.

Keeping EXPIRED rows gives:

- debugging history
- abandoned-hold visibility
- possible analytics
- explicit lifecycle semantics

without blocking future reservations.

## Clock instead of sleeps

The service depends on `java.time.Clock`.

Production uses:

~~~text
Clock.systemUTC()
~~~

Tests inject a mutable clock.

That lets expiry tests move time deterministically without waiting in real time.

## Concurrency guarantee

The RSV-120 invariant remains unchanged:

~~~text
same resource
+
overlapping active window
=> only one active reservation can commit
~~~

Temporary holds participate in the same database guarantee.

## Deliberate omissions

This skeleton does not implement:

- payment integration
- HTTP endpoints
- scheduler framework
- hold extensions
- Kafka events
- Redis
- deployment

Those concerns are orthogonal to the time/concurrency lesson.

## Result

The skeleton now covers its intended learning path:

~~~text
time interval semantics
    ↓
check-then-insert race
    ↓
database-enforced no-overlap
    ↓
temporary hold + expiry
~~~

After verification, the module can be frozen at 1.0.
