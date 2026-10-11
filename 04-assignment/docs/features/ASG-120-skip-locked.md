# ASG-120 - Lock-Aware Selection with SKIP LOCKED

## Problem from ASG-110

The old flow was:

~~~text
SELECT next PENDING job
    ↓
application decides
    ↓
UPDATE selected job to CLAIMED
~~~

Two schedulers could both select the same visible row before either claim was written.

Both callers could then receive successful assignments for the same job.

## New selection

The repository now selects with:

~~~sql
SELECT id, required_capability, priority
FROM assignment_job
WHERE status = 'PENDING'
  AND required_capability = ?
ORDER BY priority DESC, id ASC
LIMIT 1
FOR UPDATE SKIP LOCKED
~~~

The selected row is locked for update inside the surrounding service transaction.

## Transaction shape

~~~text
BEGIN

SELECT next eligible PENDING job
FOR UPDATE SKIP LOCKED
    ↓
row is now locked by this transaction
    ↓
UPDATE same row -> CLAIMED(worker)
    ↓
COMMIT
    ↓
lock released
~~~

Selection and ownership are therefore connected by one transaction boundary.

## Concurrent behavior

With:

~~~text
Job-1 priority 100
Job-2 priority 90
~~~

two concurrent schedulers behave like:

~~~text
Scheduler A                       Scheduler B

locks Job-1

                                  sees Job-1 locked
                                  skips Job-1
                                  locks Job-2

claim Job-1                       claim Job-2

COMMIT                            COMMIT
~~~

Both workers make progress without duplicate ownership.

## Why SKIP LOCKED matters

Without SKIP LOCKED, the second scheduler may wait for the first selected row even though other eligible work exists.

With SKIP LOCKED:

~~~text
locked candidate
    -> unavailable to this selector right now
    -> continue searching
~~~

That is a natural fit for queue-like work claiming.

## Why the lock must live through claim

This would still be wrong:

~~~text
transaction A:
SELECT ... FOR UPDATE
COMMIT

later:
UPDATE claim
~~~

The protection would end before ownership is recorded.

The correct boundary is:

~~~text
lock candidate
+
record claim
+
commit
~~~

inside one transaction.

## Repository contract

The method is intentionally named:

~~~text
findNextPendingForUpdate()
~~~

instead of a neutral read-like name.

The name exposes that callers receive a row with transactional locking semantics.

## Defensive claim predicate

The final update also requires:

~~~sql
WHERE id = ?
  AND status = 'PENDING'
~~~

Under the intended lock protocol exactly one row should update.

If not, the repository fails instead of silently reporting ownership.

## SKIP LOCKED is not a universal consistency mode

It intentionally gives a queue-like view:

> give me an eligible row that is not currently locked by another claimant.

It is useful for worker queues and parallel consumers.

It would be inappropriate when a caller needs a complete, globally consistent view of all matching rows.

## Contrast with 03-inventory

Inventory:

~~~text
known row + numeric invariant
    -> atomic conditional UPDATE
~~~

Assignment:

~~~text
choose one row from a candidate set
    -> lock-aware SELECT + claim
~~~

The concurrency tool follows the shape of the invariant.

## Next pressure

ASG-130 will increase the number of workers/jobs and discuss throughput, ordering, and fairness.

The correctness goal remains:

~~~text
one job
-> at most one successful claimant
~~~
