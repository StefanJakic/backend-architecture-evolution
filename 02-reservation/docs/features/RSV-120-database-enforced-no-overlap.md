# RSV-120 - Database-Enforced No-Overlap

## Problem from RSV-110

The service performed:

~~~text
SELECT overlap
if none:
    INSERT
~~~

Under READ COMMITTED two transactions could both observe no committed conflict and both insert overlapping reservations.

## New rule ownership

The application still performs the overlap SELECT for fast sequential rejection.

Correctness no longer depends on it.

PostgreSQL now owns the final invariant:

~~~text
same resource
AND
overlapping time window
=> invalid database state
~~~

## Exclusion constraint

The migration enables `btree_gist` and adds:

~~~sql
EXCLUDE USING gist (
    resource_id WITH =,
    (tstzrange(starts_at, ends_at, '[)')) WITH &&
)
~~~

The operators mean:

~~~text
resource_id WITH =
    conflict only for the same resource

time range WITH &&
    conflict when the ranges overlap
~~~

The range uses the same half-open boundary as the domain:

~~~text
[start,end)
~~~

So adjacent reservations remain valid.

## Concurrent behavior

The test still deliberately forces both transactions through the application check:

~~~text
Tx A: SELECT overlap -> false
Tx B: SELECT overlap -> false
~~~

Then both attempt INSERT.

The database guarantees:

~~~text
one INSERT/transaction commits
one INSERT receives exclusion violation
~~~

The persistence adapter translates PostgreSQL SQLSTATE:

~~~text
23P01 = exclusion_violation
~~~

into:

~~~text
ReservationConflictException
~~~

Application code does not need to know the database error code.

## Why keep the SELECT at all?

It gives a fast and understandable response for ordinary sequential conflicts.

But it is only advisory validation.

The distinction is:

~~~text
application check
    -> good UX / early rejection

database constraint
    -> correctness under concurrency
~~~

## Why this is stronger than synchronized Java code

A JVM lock protects only one process.

The database constraint protects the invariant across:

- multiple threads
- multiple application instances
- multiple Pods
- any writer using the same database

## Why not optimistic locking?

Optimistic locking is natural when two transactions modify the same existing row.

Here they create different rows.

The conflict is a predicate across a set of rows and a time range.

## Why not SERIALIZABLE as the first solution?

Serializable isolation can also protect predicate-based decisions, but callers must handle serialization failures and retry whole transactions.

The exclusion constraint directly expresses the business invariant in the data model and gives a deterministic conflict at the invalid write.

Serializable remains a valid alternative when the invariant cannot be represented cleanly as a constraint.

## Result

The same concurrency pressure evolves from:

~~~text
RSV-110
2 successful commits
=> double booking
~~~

to:

~~~text
RSV-120
1 successful commit
1 ReservationConflictException
=> invariant preserved
~~~

## Next pressure

A real booking flow often needs time to complete payment or confirmation.

RSV-130 introduces a temporary hold:

~~~text
AVAILABLE
  -> HELD until expiresAt
  -> CONFIRMED
or
  -> expires and becomes available again
~~~

That adds a new dimension: a reservation can block a time window temporarily without being permanently confirmed.
