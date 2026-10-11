# 02 - Reservation / Time Window

## Status

**0.3.0-SNAPSHOT - Database-Enforced No-Overlap**

This is a focused learning skeleton, intentionally much smaller than `01-lifecycle`.

## Dominant problem

~~~text
Can this resource be reserved during this time window
even when multiple transactions compete?
~~~

The core mental model is **time + concurrency**.

## Interval semantics

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

So exact adjacency is valid.

## Evolution so far

### RSV-101

Model the time invariant correctly in Java.

### RSV-110

Persist it and prove this race:

~~~text
Tx A: SELECT overlap -> false
Tx B: SELECT overlap -> false
Tx A: INSERT
Tx B: INSERT
=> double booking
~~~

### RSV-120

Move the final invariant into PostgreSQL:

~~~text
EXCLUDE USING gist (
    resource_id WITH =,
    time_range WITH &&
)
~~~

Now the same race becomes:

~~~text
Tx A: check -> false
Tx B: check -> false

both INSERT

=> 1 success
=> 1 conflict
=> 1 stored reservation
~~~

## Key distinction

~~~text
application validation
    = early / friendly rejection

database constraint
    = concurrency correctness
~~~

The application still checks for overlap.

It no longer relies on that check as the guarantee.

## Why PostgreSQL fits this invariant

The conflict is not two updates to one Reservation row.

It is:

~~~text
same resource
+
overlapping range
+
different rows
~~~

That is why ordinary optimistic locking is not the natural solution here.

## Current architecture

~~~text
ReservationService
      |
      | advisory overlap check
      v
ReservationRepository
      |
      v
PostgreSQL
      |
      +-- CHECK start < end
      +-- GiST exclusion constraint
          same resource + overlapping range forbidden
~~~

## Run

~~~bash
podman compose up -d
gradle test
~~~

## Remaining scope

~~~text
RSV-101  Core time-window model
RSV-110  PostgreSQL + race
RSV-120  Database-enforced no-overlap    <- current
RSV-130  Temporary hold + expiry
1.0      Freeze
~~~

No HTTP, Kafka, Redis, outbox, or Kubernetes will be added to this skeleton.

## Next

RSV-130 asks a different time question:

> What if a user needs five minutes to finish payment before the booking becomes permanent?

That introduces temporary holds and expiry without reopening the already-solved double-booking problem.
