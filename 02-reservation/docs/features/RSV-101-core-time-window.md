# RSV-101 - Core Time Window Model

## Dominant problem

Reservations are primarily a **time-range** problem.

Before persistence or locking, the model must define exactly when two intervals conflict.

## Interval convention

This skeleton uses half-open intervals:

~~~text
[start, end)
~~~

The start is included and the end is excluded.

Therefore:

~~~text
10:00 ───── 11:00
             11:00 ───── 12:00
~~~

are adjacent and valid.

## Overlap rule

Two windows overlap when:

~~~text
A.start < B.end
AND
B.start < A.end
~~~

This handles:

- partial overlap
- one interval containing another
- identical intervals

while allowing exact boundary adjacency.

## Resource scope

Overlap matters only for the same scarce resource.

Two rooms may be reserved for the same time.

One room may not have two overlapping reservations.

## Why start without Spring?

The first lesson is domain semantics, not framework configuration.

RSV-110 will introduce PostgreSQL and show why this apparently correct sequence is still unsafe under concurrency:

~~~text
check that no overlap exists
then insert reservation
~~~

Two transactions can both pass the check before either INSERT becomes visible.
