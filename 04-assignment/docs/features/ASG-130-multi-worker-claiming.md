# ASG-130 - Multi-Worker Claiming and Fairness

## Goal

ASG-120 proved that two concurrent schedulers can claim two different jobs with `FOR UPDATE SKIP LOCKED`.

ASG-130 increases the pressure:

~~~text
pending jobs = 10
concurrent workers = 20
~~~

Expected:

~~~text
10 successful assignments
10 no-job outcomes
10 unique claimed job IDs
0 duplicate ownership
~~~

## Strong invariant

The important accounting rule is:

~~~text
successful assignment count
=
unique claimed job count
~~~

and every returned assignment must agree with the final database claimant.

This connects externally visible ownership to actual persisted ownership.

## Common start gate

All workers are prepared first:

~~~text
20 workers
    ↓
ready barrier
    ↓
release together
~~~

They then compete through the same lock-aware selection path.

## What SKIP LOCKED guarantees here

Each transaction asks for:

~~~text
highest-priority eligible PENDING row
that is not currently locked
~~~

Once one transaction locks a job, another worker does not select the same row.

It either:

~~~text
takes another available row
~~~

or:

~~~text
finds no currently claimable row
~~~

and returns a no-job outcome.

## What SKIP LOCKED does not guarantee

It does not provide strict global fairness.

Suppose:

~~~text
Job-1 priority 100
Job-2 priority 90
Job-3 priority 80
~~~

If Job-1 is currently locked, another transaction may legitimately choose Job-2.

So under concurrency:

~~~text
ORDER BY priority DESC
+
SKIP LOCKED
~~~

means:

> choose the highest-priority row currently available to this transaction

It does not mean:

> every worker globally participates in one perfectly serialized priority queue.

## Throughput vs fairness

SKIP LOCKED favors parallel progress:

~~~text
locked work
-> skip
-> continue
~~~

That is often desirable for worker queues.

But systems that require stronger fairness may need different architecture:

- explicit queue sequencing
- partition ownership
- leases
- admission control
- fairness metadata
- starvation detection

Those are outside this focused skeleton.

## Crash semantics

The row lock itself is transactional.

If a process dies before commit, PostgreSQL rolls back the transaction and releases the lock.

But once a job is committed as CLAIMED, handling a worker that later disappears requires a separate lifecycle concept such as:

~~~text
lease
heartbeat
timeout
requeue
~~~

That is intentionally not added here because it introduces a new dominant pressure.

## Final lesson

The assignment path is now:

~~~text
eligibility
    ↓
deterministic selection
    ↓
duplicate select-then-claim race
    ↓
FOR UPDATE SKIP LOCKED
    ↓
multi-worker uniqueness verification
~~~

The important guarantee is:

~~~text
one job
-> at most one successful claimant
~~~

After local verification, only the 1.0 freeze remains.
