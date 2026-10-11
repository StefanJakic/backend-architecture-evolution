# 02 - Reservation / Time Window

## Status

**0.4.0-SNAPSHOT - Temporary Hold + Expiry**

This is a focused learning skeleton, intentionally much smaller than `01-lifecycle`.

## Dominant problem

~~~text
How do we protect a scarce resource across time
when multiple transactions compete and bookings can be temporary?
~~~

## Core interval rule

Reservations use half-open windows:

~~~text
[start, end)
~~~

Two windows overlap when:

~~~text
A.start < B.end
AND
B.start < A.end
~~~

Exact adjacency is valid.

## Evolution

### RSV-101 - Time model

Model overlap correctly in Java.

### RSV-110 - Race

Prove that:

~~~text
check overlap
then insert
~~~

can double-book under concurrency even inside transactions.

### RSV-120 - Database invariant

PostgreSQL GiST exclusion constraint changes:

~~~text
2 concurrent overlapping commits
~~~

into:

~~~text
1 success
1 conflict
~~~

### RSV-130 - Hold + expiry

Add:

~~~text
HELD
CONFIRMED
EXPIRED
~~~

A hold has an `expiresAt`.

~~~text
HELD before deadline
    -> blocks slot

HELD after deadline
    -> EXPIRED
    -> releases slot

HELD confirmed before deadline
    -> CONFIRMED
    -> keeps blocking
~~~

## Active database invariant

Only active rows participate in no-overlap enforcement:

~~~text
HELD       -> active
CONFIRMED  -> active
EXPIRED    -> inactive
~~~

Conceptually:

~~~sql
EXCLUDE USING gist (...)
WHERE status IN ('HELD', 'CONFIRMED')
~~~

The application still performs an overlap SELECT for early rejection.

PostgreSQL remains the final concurrency guarantee.

## Expiry strategy

Before a new booking decision:

~~~text
expire old HELD rows
    ↓
check overlap
    ↓
attempt write
~~~

There is also an explicit:

~~~text
expireExpiredHolds()
~~~

operation that a future scheduler could call.

No scheduler framework is added here.

## Why Clock?

The service receives `java.time.Clock`.

Production:

~~~text
Clock.systemUTC()
~~~

Tests use a mutable Clock so expiry is deterministic and does not require sleeps.

## Current architecture

~~~text
ReservationService
      |
      +-- Clock
      |
      v
ReservationRepository
      |
      v
PostgreSQL
      |
      +-- reservation state
      +-- expires_at
      +-- partial GiST exclusion constraint
~~~

## Run

~~~bash
podman compose up -d
./gradlew test
~~~

If the wrapper is not yet committed in this module, use the wrapper you generated locally or run with a compatible Gradle version.

## Scope complete

The intended feature path is now complete:

~~~text
RSV-101  Core time-window model
RSV-110  PostgreSQL + check-then-insert race
RSV-120  Database-enforced no-overlap
RSV-130  Temporary hold + expiry          <- current
~~~

After this branch is verified, only a tiny **1.0 freeze** remains.

No HTTP, Kafka, Redis, outbox, Kubernetes, or AWS layers will be added to this skeleton.
