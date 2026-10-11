# 04 - Assignment / Matching

## Status

**0.2.0-SNAPSHOT - PostgreSQL Select-Then-Claim Race**

This focused skeleton studies selection and exclusive claiming.

## Dominant question

> When several pieces of work are eligible, how do we select and claim exactly one without duplicate assignment?

## 1. Deterministic local selection

ASG-101 established:

~~~text
eligible jobs only
    ↓
highest priority
    ↓
stable ID tie-breaker
~~~

This makes local choice reproducible.

## 2. Persistence introduces ownership

Jobs now live in PostgreSQL:

~~~text
PENDING
    ↓
CLAIMED(worker)
~~~

Current service flow is deliberately naive:

~~~text
SELECT next PENDING job
    ↓
application receives candidate
    ↓
UPDATE job as CLAIMED
~~~

## 3. Concurrent bug

With one pending job:

~~~text
Scheduler A SELECT -> Job-1
Scheduler B SELECT -> Job-1

A claims for Worker-A
B claims for Worker-B
~~~

Both service calls succeed:

~~~text
Assignment(Job-1, Worker-A)
Assignment(Job-1, Worker-B)
~~~

The final row contains only one claimant because the last update wins.

That does not undo the fact that two workers were already told they owned the same job.

## Key lesson

~~~text
deterministic selection
!=
exclusive claiming
~~~

and:

~~~text
valid final row
!=
valid claim history
~~~

## Why this differs from inventory

Inventory had one known counter:

~~~text
UPDATE quantity
WHERE quantity >= requested
~~~

Assignment must choose from a **set of eligible candidate rows**.

That makes row-lock-aware selection a natural next tool.

## Evolution

~~~text
ASG-101  Eligibility + deterministic selection
ASG-110  PostgreSQL select-then-claim race    <- current
ASG-120  SELECT FOR UPDATE SKIP LOCKED
ASG-130  Multi-worker claiming / fairness notes
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

ASG-120 will select the next pending job using:

~~~sql
FOR UPDATE SKIP LOCKED
~~~

so a scheduler does not select a candidate row already being claimed by another transaction.
