# ASG-140 - Assignment Skeleton 1.0 Freeze

## Status

Complete.

## Scope

No new business behavior is introduced in this release.

The purpose of 1.0 is to mark the selection/claiming objective as complete and prevent this module from growing into worker-lifecycle or distributed-queue concerns.

## Completed pressures

~~~text
eligibility
    ↓
deterministic selection
    ↓
select-then-claim race
    ↓
FOR UPDATE SKIP LOCKED
    ↓
multi-worker uniqueness verification
~~~

## Why freeze now?

The skeleton can answer the questions it was created to teach:

- why does deterministic selection not imply ownership?
- how can two schedulers both correctly select the same job?
- why can the final row hide duplicate ownership already returned to callers?
- why must the row lock survive until the claim is persisted?
- why is SKIP LOCKED useful for queue-like worker claiming?
- why does SKIP LOCKED improve progress without guaranteeing strict global fairness?
- why is this different from the atomic-counter solution in inventory?

Adding leases, heartbeat expiry, requeue, worker crash recovery, messaging, or distributed orchestration would introduce new dominant problems.

## Final ownership invariant

~~~text
one job
-> at most one successful claimant
~~~

Under multi-worker contention:

~~~text
successful assignment count
=
unique claimed job count
~~~

## Strategy lesson

~~~text
known row + simple numeric invariant
    -> atomic conditional UPDATE

select one candidate from a set
    -> FOR UPDATE SKIP LOCKED
~~~

Concurrency control should follow the shape of the invariant.

## Next pressure

The next skeleton moves from claiming to behavior variation:

~~~text
05-policy

same operation
different business policy

Where should behavior variation live?
~~~
