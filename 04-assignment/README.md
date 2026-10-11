# 04 - Assignment / Matching

## Status

**0.4.0-SNAPSHOT - Multi-Worker Claiming**

This focused skeleton studies selection and exclusive claiming.

## Dominant question

> When several pieces of work are eligible, how do we select and claim exactly one without duplicate assignment?

## Learning path

~~~text
ASG-101  Eligibility + deterministic selection
    ↓
ASG-110  Select-then-claim race
    ↓
ASG-120  SELECT FOR UPDATE SKIP LOCKED
    ↓
ASG-130  Multi-worker claiming / fairness
~~~

## 1. Deterministic selection

~~~text
eligible jobs only
    ↓
highest priority
    ↓
stable ID tie-breaker
~~~

This makes local choice reproducible.

It does not create ownership.

## 2. Duplicate assignment race

The naive persistence flow:

~~~text
SELECT next PENDING
    ↓
UPDATE later
~~~

allowed two schedulers to select the same job and both return successful ownership.

## 3. Lock-aware selection

Claiming now uses:

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

## 4. Multi-worker verification

The final pressure test uses:

~~~text
10 pending jobs
20 concurrent workers
~~~

Required result:

~~~text
10 successful assignments
10 no-job outcomes
10 unique claimed job IDs
0 duplicates
~~~

Strong invariant:

~~~text
successful assignment count
=
unique claimed job count
~~~

Every successful assignment must also match the final database claimant.

## Ordering and fairness

`ORDER BY priority DESC` still matters.

But with `SKIP LOCKED`, a worker selects the highest-priority row **currently available to its transaction**.

A higher-priority row that is already locked can be skipped in favor of the next one.

Therefore:

~~~text
priority ordering
!=
strict global fairness under concurrency
~~~

SKIP LOCKED is primarily a throughput/progress mechanism for queue-like workloads.

## Crash boundary

Before commit:

~~~text
process dies
-> transaction rolls back
-> row lock is released
~~~

After CLAIMED is committed, recovering work from a dead worker requires a different lifecycle:

~~~text
lease / heartbeat / timeout / requeue
~~~

That is intentionally outside this skeleton.

## Contrast with inventory

~~~text
03 inventory:
known hot counter
-> atomic conditional UPDATE

04 assignment:
select one row from a candidate set
-> FOR UPDATE SKIP LOCKED
~~~

The concurrency mechanism follows the shape of the invariant.

## Evolution

~~~text
ASG-101  Eligibility + deterministic selection
ASG-110  PostgreSQL select-then-claim race
ASG-120  SELECT FOR UPDATE SKIP LOCKED
ASG-130  Multi-worker claiming / fairness      <- current
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

After this feature is verified, only the **1.0 freeze** remains.
