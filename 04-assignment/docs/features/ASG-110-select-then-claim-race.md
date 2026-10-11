# ASG-110 - PostgreSQL Select-Then-Claim Race

## Goal

Move assignment into shared PostgreSQL state and show why selecting a candidate is not the same as owning it.

## Naive transaction

The service currently performs:

~~~text
BEGIN

SELECT next PENDING job
ORDER BY priority DESC, id ASC
    ↓
return Job to application
    ↓
UPDATE job
SET status = CLAIMED,
    claimed_by = worker
WHERE id = ?

COMMIT
~~~

Sequentially, this looks correct.

## Deterministic race

Start with:

~~~text
Job-1 = PENDING
~~~

Then force:

~~~text
Scheduler A                       Scheduler B

SELECT -> Job-1

                                  SELECT -> Job-1

[wait]                            [wait]

claim for Worker-A                claim for Worker-B

COMMIT                            COMMIT
~~~

Both service calls return success:

~~~text
Assignment(Job-1, Worker-A)
Assignment(Job-1, Worker-B)
~~~

The system has told two different workers that they own the same job.

## Why the final row can hide the bug

The database finishes with only one row:

~~~text
Job-1
status = CLAIMED
claimed_by = Worker-A or Worker-B
~~~

The last update wins.

Looking only at final state can therefore hide the externally visible duplicate assignment that already occurred.

## Why row constraints do not solve it

The table enforces:

~~~text
PENDING -> claimed_by must be null
CLAIMED -> claimed_by must be non-null
~~~

Every individual stored state is valid.

But those constraints do not guarantee:

~~~text
one successful claim in history
~~~

This is similar to inventory's lesson:

~~~text
valid final row
!=
valid concurrent business outcome
~~~

but the shape of the problem is different.

Inventory competed over one numeric counter.

Assignment competes over selection from a set of candidate rows.

## Why deterministic ordering is still useful

Both schedulers selecting Job-1 is not a bug in the ordering.

They see the same visible state and correctly choose:

~~~text
highest priority
then stable ID
~~~

The missing property is exclusive ownership during selection.

## Why a conditional UPDATE alone is not the whole lesson

One possible repair would be:

~~~sql
UPDATE assignment_job
SET status = 'CLAIMED',
    claimed_by = ?
WHERE id = ?
  AND status = 'PENDING'
~~~

and then inspect the affected row count.

That can safely claim a **known** job.

But assignment usually asks a broader question:

> From many eligible pending jobs, which available row should this worker take next?

ASG-120 therefore uses PostgreSQL row locking during candidate selection:

~~~text
SELECT next eligible PENDING row
FOR UPDATE SKIP LOCKED
~~~

This lets concurrent workers naturally skip work another scheduler is already claiming.

## Next pressure

ASG-120 will combine:

~~~text
selection
+
exclusive row claim
~~~

inside one transaction.

Expected behavior with two jobs and two concurrent workers:

~~~text
Worker-A -> Job-1
Worker-B -> Job-2
~~~

rather than both selecting Job-1.
