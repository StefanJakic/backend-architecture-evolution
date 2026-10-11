# RSV-110 - PostgreSQL and the Check-Then-Insert Race

## Goal

Move the reservation rule from one in-memory calendar into PostgreSQL and ask whether a normal transaction is enough to protect it.

## Naive service

The application does:

~~~text
BEGIN

SELECT whether an overlapping reservation exists

if none exists:
    INSERT reservation

COMMIT
~~~

The service uses PostgreSQL READ COMMITTED.

For sequential requests this behaves correctly.

## Same overlap rule

The SQL predicate is the same rule used by the Java `TimeWindow`:

~~~sql
starts_at < requested_end
AND
ends_at > requested_start
~~~

So the bug is not caused by inconsistent business logic.

## Deterministic race

The concurrency test forces this interleaving:

~~~text
Transaction A                         Transaction B

BEGIN                                 BEGIN

SELECT overlap
-> false

                                      SELECT overlap
                                      -> false

[wait]                                [wait]

INSERT reservation                    INSERT reservation

COMMIT                                COMMIT
~~~

Final state:

~~~text
two overlapping reservations
for the same resource
~~~

Both service calls made the correct decision from the committed data visible at the moment of their SELECT.

The invariant is still broken.

## Why @Transactional is not enough

A transaction gives atomicity to the statements inside one transaction.

It does not automatically serialize this predicate:

> no overlapping row exists

Under READ COMMITTED, one transaction does not see another transaction's uncommitted INSERT.

Both overlap checks can therefore return false.

## Why ordinary optimistic locking does not naturally solve this

Optimistic locking usually detects two transactions updating the same existing row/version.

Here the conflict is different:

~~~text
Tx A inserts new reservation A
Tx B inserts new reservation B
~~~

There is no shared Reservation row whose version both transactions update.

The invariant spans a **set of rows for one resource and time range**.

## Why SELECT ... FOR UPDATE is also not automatically enough

If the overlap query returns no rows, there may be nothing to lock.

Locking existing conflicting reservations cannot protect the absence of a row by itself.

A design can introduce a stable parent resource row and lock that, but that serializes booking attempts per resource.

## What RSV-120 will fix

The next feature will move the no-overlap invariant into a concurrency mechanism PostgreSQL can enforce at commit/write time.

The main candidate is a PostgreSQL range/exclusion constraint:

~~~text
same resource
AND
overlapping time range
=> database conflict
~~~

We will then rerun the concurrent scenario and require that only one reservation can commit.
