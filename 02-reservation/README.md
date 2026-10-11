# 02 - Reservation / Time Window

## Status

**0.1.0-SNAPSHOT - Core Time Window Model**

This is a focused learning skeleton, intentionally much smaller than `01-lifecycle`.

## Dominant problem

~~~text
Can this resource be reserved during this time window?
~~~

The core mental model is **time overlap**, not lifecycle state.

## Current rule

Time windows use half-open interval semantics:

~~~text
[start, end)
~~~

So:

~~~text
10:00-11:00
11:00-12:00
~~~

do not overlap.

Two windows overlap when:

~~~text
A.start < B.end
AND
B.start < A.end
~~~

## Current scope

- immutable TimeWindow
- explicit overlap semantics
- same-resource conflict rule
- adjacent reservations allowed
- pure Java + JUnit

No Spring or database yet.

## Planned evolution

~~~text
RSV-101  Core time-window model
RSV-110  PostgreSQL + check-then-insert race
RSV-120  Database-enforced no-overlap + concurrency
RSV-130  Temporary hold + expiry
1.0      Freeze
~~~

## Run

~~~bash
gradle test
~~~

## Next pressure

The in-memory calendar is correct only inside one process and one thread of ownership.

Next we persist reservations and ask:

> What happens when two transactions both check availability before either one inserts?
