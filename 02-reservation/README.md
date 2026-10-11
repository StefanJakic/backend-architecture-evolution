# 02 - Reservation / Time Window

## Status

**0.2.0-SNAPSHOT - PostgreSQL / Check-Then-Insert Race**

This is a focused learning skeleton, intentionally much smaller than `01-lifecycle`.

## Dominant problem

~~~text
Can this resource be reserved during this time window?
~~~

The core mental model is **time + concurrency**.

## Interval semantics

Reservations use half-open time windows:

~~~text
[start, end)
~~~

Two windows overlap when:

~~~text
A.start < B.end
AND
B.start < A.end
~~~

Exact adjacency is allowed.

## Current architecture

~~~text
ReservationService
      |
      v
ReservationRepository
      |
      v
PostgreSQL
~~~

No HTTP layer is added because it would not teach anything new for this skeleton.

## The bug we intentionally preserve

The service currently performs:

~~~text
BEGIN

check overlap
  ↓
none found
  ↓
insert reservation

COMMIT
~~~

Sequentially, that is correct.

Concurrently:

~~~text
Tx A: check -> false
Tx B: check -> false
Tx A: insert -> success
Tx B: insert -> success

=> double booking
~~~

The test suite deliberately reproduces this race deterministically.

## Why this matters

`@Transactional` does not mean:

~~~text
nobody else can make the same decision concurrently
~~~

At PostgreSQL READ COMMITTED, each transaction can correctly observe that no committed overlap exists before either INSERT becomes visible.

## Evolution

~~~text
RSV-101  Core time-window model
RSV-110  PostgreSQL + check-then-insert race   <- current
RSV-120  Database-enforced no-overlap
RSV-130  Temporary hold + expiry
1.0      Freeze
~~~

## Local database

~~~bash
podman compose up -d
gradle test
~~~

## Next pressure

RSV-120 will change the invariant from:

~~~text
application checks first
~~~

to:

~~~text
database refuses an invalid concurrent final state
~~~

and the same concurrency scenario must produce one successful reservation and one conflict instead of two committed overlaps.
