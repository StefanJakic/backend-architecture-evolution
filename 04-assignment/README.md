# 04 - Assignment / Matching

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton studies one dominant backend problem:

> When several pieces of work are eligible, how do we select and claim exactly one without duplicate assignment?

## Final learning path

~~~text
ASG-101  Eligibility + deterministic selection
    ↓
ASG-110  PostgreSQL select-then-claim race
    ↓
ASG-120  SELECT FOR UPDATE SKIP LOCKED
    ↓
ASG-130  Multi-worker claiming / fairness
    ↓
1.0      Freeze
~~~

## 1. Eligibility and deterministic selection

The local model begins with:

~~~text
Worker
  id
  capability

Job
  id
  requiredCapability
  priority
~~~

Eligibility:

~~~text
worker.capability
==
job.requiredCapability
~~~

Selection order:

~~~text
eligible jobs only
    ↓
highest priority
    ↓
stable ID tie-breaker
~~~

The stable ordering makes local decisions reproducible.

It does not create ownership.

## 2. Why selection is not claiming

The first persistence design was:

~~~text
BEGIN

SELECT next PENDING job
ORDER BY priority DESC, id ASC
    ↓
return candidate to application
    ↓
UPDATE job to CLAIMED

COMMIT
~~~

Under READ COMMITTED, two schedulers can both observe the same pending row:

~~~text
Job-1 = PENDING

Scheduler A SELECT -> Job-1
Scheduler B SELECT -> Job-1

A claims for Worker-A
B claims for Worker-B
~~~

Both callers can receive:

~~~text
Assignment(Job-1, Worker-A)
Assignment(Job-1, Worker-B)
~~~

even though only one job existed.

The final database row keeps only one claimant, so it can hide the fact that duplicate ownership was already returned to callers.

The lesson is:

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

## 3. Lock-aware selection

The fix moves ownership protection into candidate selection:

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

Transaction shape:

~~~text
BEGIN

SELECT next eligible PENDING job
FOR UPDATE SKIP LOCKED
    ↓
row locked
    ↓
UPDATE same row -> CLAIMED(worker)
    ↓
COMMIT
    ↓
lock released
~~~

The important boundary is:

~~~text
select
+
lock
+
claim
+
commit
~~~

inside one transaction.

## 4. Why SKIP LOCKED fits assignment

Without SKIP LOCKED, a second worker may wait on the highest-ranked locked row even when other eligible jobs exist.

With SKIP LOCKED:

~~~text
best row locked?
    -> skip it
    -> try next eligible row
~~~

That is a natural model for parallel worker queues.

Example:

~~~text
Job-1 priority 100
Job-2 priority 90
~~~

Concurrent result:

~~~text
Worker-A locks Job-1
Worker-B skips Job-1
Worker-B locks Job-2

Worker-A -> Job-1
Worker-B -> Job-2
~~~

## 5. Multi-worker verification

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
0 duplicate ownership
~~~

The stronger accounting invariant is:

~~~text
successful assignment count
=
unique claimed job count
~~~

Every returned assignment must also agree with the final database claimant.

## 6. Ordering vs fairness

Priority ordering still matters:

~~~text
ORDER BY priority DESC
~~~

But with SKIP LOCKED, a transaction selects the best candidate **currently available to it**.

If a higher-priority row is already locked, another worker may legitimately claim the next one.

Therefore:

~~~text
priority ordering
!=
strict global fairness under concurrency
~~~

SKIP LOCKED primarily optimizes parallel progress.

It does not provide a perfectly serialized global priority queue.

## 7. Crash boundary

Before commit:

~~~text
process dies
-> transaction rolls back
-> row lock is released
~~~

After CLAIMED has committed, recovering work from a worker that later disappears would require a different lifecycle:

~~~text
lease
heartbeat
timeout
requeue
~~~

That is intentionally outside this skeleton.

## 8. Contrast with inventory

The previous skeleton used a different concurrency mechanism because the invariant shape was different.

~~~text
03 inventory:
known hot counter
-> atomic conditional UPDATE

04 assignment:
choose one row from a candidate set
-> FOR UPDATE SKIP LOCKED
~~~

The tool follows the shape of the problem.

## Final architecture

~~~text
AssignmentService
      |
      | claimNext(worker)
      v
AssignmentRepository
      |
      | findNextPendingForUpdate()
      v
PostgreSQL
      |
      +-- ORDER BY priority
      +-- FOR UPDATE SKIP LOCKED
      +-- claim before transaction commit
~~~

## What this skeleton teaches

- eligibility filtering
- deterministic candidate ordering
- selection vs ownership
- READ COMMITTED select-then-update races
- duplicate assignment despite valid final state
- row-level locking
- FOR UPDATE
- SKIP LOCKED
- queue-like concurrent claiming
- uniqueness under multi-worker contention
- throughput vs strict fairness
- transaction boundaries around ownership

## Deliberately not included

This module does not add:

- HTTP API
- Kafka
- Redis
- leases
- heartbeats
- claim expiry
- requeue logic
- distributed worker orchestration
- Kubernetes
- AWS
- benchmark claims

Those are different architecture pressures.

## Run

~~~bash
./gradlew test
~~~

## Freeze rule

This module is intentionally closed at **1.0.0**.

A new architecture pressure belongs in a new skeleton.

## Next

**05 - Behavior / Policy**

Dominant question:

> When the same use case must behave differently under different business policies, where should that variation live without turning the service into a branch jungle?
