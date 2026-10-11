# 02 - Reservation / Time Window

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton explores one dominant backend problem:

> How do we protect a scarce resource across time when multiple transactions compete and bookings can be temporary?

Unlike `01-lifecycle`, this module deliberately stops once that problem is understood.

## Final learning path

~~~text
RSV-101  Time-window semantics
    ↓
RSV-110  Check-then-insert race
    ↓
RSV-120  Database-enforced no-overlap
    ↓
RSV-130  Temporary hold + expiry
    ↓
1.0      Freeze
~~~

## 1. Time-window semantics

Reservations use half-open intervals:

~~~text
[start, end)
~~~

Two windows overlap when:

~~~text
A.start < B.end
AND
B.start < A.end
~~~

Therefore:

~~~text
10:00-11:00
11:00-12:00
~~~

are adjacent rather than conflicting.

## 2. Why application validation is not concurrency control

The naive transaction was:

~~~text
SELECT whether overlap exists
if none:
    INSERT reservation
~~~

Under READ COMMITTED:

~~~text
Tx A: SELECT -> no conflict
Tx B: SELECT -> no conflict

Tx A: INSERT
Tx B: INSERT
~~~

Both decisions can be locally correct while the final state is globally invalid.

The deterministic concurrency test exists specifically to prove this interleaving.

## 3. Database-enforced invariant

PostgreSQL becomes the final arbiter with a GiST exclusion constraint.

Conceptually:

~~~text
same resource
AND
overlapping active time range
=> reject write
~~~

The application overlap SELECT remains useful for early rejection.

It is not the correctness guarantee.

This distinction is central:

~~~text
application validation
    = UX / fast fail

database constraint
    = concurrency correctness
~~~

## 4. Temporary holds

Reservations evolve into:

~~~text
HELD
  -> CONFIRMED
or
  -> EXPIRED
~~~

Semantics:

~~~text
HELD       -> blocks capacity temporarily
CONFIRMED  -> blocks capacity permanently
EXPIRED    -> remains stored, no longer blocks
~~~

The exclusion constraint applies only to active states:

~~~text
HELD
CONFIRMED
~~~

so expired historical rows remain in the database without consuming capacity.

## 5. Expiry is explicit state

Expiry is not hidden inside a query using wall-clock time.

Instead:

~~~text
HELD + expiresAt <= now
    ↓
EXPIRED
~~~

The service uses `java.time.Clock` so tests can advance time deterministically.

No `Thread.sleep()` is required.

## 6. Transaction-boundary lesson

Expiry cleanup originally happened inside the same transaction as `confirmHold()`.

That created this bug:

~~~text
BEGIN
mark HELD -> EXPIRED
confirm fails
ROLLBACK

=> reservation becomes HELD again
~~~

The final design performs expiry in a separate `REQUIRES_NEW` transaction:

~~~text
expire stale holds
    ↓
COMMIT

then
    ↓
reserve / hold / confirm
~~~

This makes expiry durable maintenance state even if the command that triggered cleanup later fails.

## Final architecture

~~~text
ReservationService
      |
      +-- Clock
      |
      +-- ReservationExpiryService
      |       |
      |       +-- REQUIRES_NEW
      |
      v
ReservationRepository
      |
      v
PostgreSQL
      |
      +-- start < end CHECK
      +-- status / expires_at
      +-- partial GiST exclusion constraint
~~~

## What this skeleton teaches

- half-open interval modeling
- overlap predicates
- READ COMMITTED visibility
- deterministic concurrency testing
- why `@Transactional` is not automatic serialization
- why optimistic locking does not naturally solve competing INSERTs
- PostgreSQL range types
- GiST exclusion constraints
- database-enforced invariants
- partial constraints for active state
- temporary holds
- expiry
- `Clock`-driven time testing
- transaction propagation and `REQUIRES_NEW`

## Deliberately not included

This module does not add:

- HTTP API
- Kafka
- Redis
- outbox
- payments
- scheduler framework
- Kubernetes
- AWS deployment

Those would repeat lessons from other skeletons without improving the core time/concurrency model.

## Run

~~~bash
podman compose up -d
./gradlew test
~~~

The Gradle wrapper is committed and pinned to Gradle 8.14.3.

## Freeze rule

This module is intentionally closed at **1.0.0**.

A new architecture pressure belongs in a new skeleton.

## Next

**03 - Scarce Resource / Inventory**

Dominant question:

> How do we prevent overselling when many requests decrement the same quantity concurrently?
