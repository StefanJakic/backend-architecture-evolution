# 04 - Assignment / Matching

## Status

**0.3.0-SNAPSHOT - Lock-Aware Selection with SKIP LOCKED**

This focused skeleton studies selection and exclusive claiming.

## Dominant question

> When several pieces of work are eligible, how do we select and claim exactly one without duplicate assignment?

## Evolution so far

### ASG-101 - Deterministic selection

~~~text
eligible jobs only
    ↓
highest priority
    ↓
stable ID tie-breaker
~~~

This makes local choice reproducible.

### ASG-110 - Select-then-claim race

The naive persistence flow:

~~~text
SELECT next PENDING
    ↓
UPDATE later
~~~

allowed two schedulers to select the same job and both return successful ownership.

### ASG-120 - Lock-aware candidate selection

Selection now uses:

~~~sql
SELECT id, required_capability, priority
FROM assignment_job
WHERE status = 'PENDING'
  AND required_capability = ?
ORDER BY priority DESC, id ASC
LIMIT 1
FOR UPDATE SKIP LOCKED
~~~

The selected row remains locked until the same transaction records the claim and commits.

## Concurrent result

With two eligible jobs:

~~~text
Job-1 priority 100
Job-2 priority 90
~~~

and two schedulers:

~~~text
A locks Job-1
B skips Job-1
B locks Job-2

A -> Assignment(Job-1, Worker-A)
B -> Assignment(Job-2, Worker-B)
~~~

The exact worker/job pairing may vary by scheduling, but the job IDs must be different.

## Key lesson

~~~text
selection
+
transactional row lock
+
claim before commit
=
exclusive ownership
~~~

## Why SKIP LOCKED?

It avoids unnecessary waiting when another eligible candidate exists.

~~~text
best row locked?
    -> skip it
    -> try next eligible row
~~~

That is useful for queue-like claiming.

## Contrast with inventory

~~~text
03 inventory:
known hot counter
-> atomic conditional UPDATE

04 assignment:
select one candidate from a set
-> FOR UPDATE SKIP LOCKED
~~~

Different invariant shape, different concurrency mechanism.

## Evolution

~~~text
ASG-101  Eligibility + deterministic selection
ASG-110  PostgreSQL select-then-claim race
ASG-120  SELECT FOR UPDATE SKIP LOCKED          <- current
ASG-130  Multi-worker claiming / fairness notes
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

ASG-130 will run several workers against several pending jobs and verify that claimed job IDs stay unique while discussing ordering and fairness under concurrent SKIP LOCKED workers.
