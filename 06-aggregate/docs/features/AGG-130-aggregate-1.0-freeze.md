# AGG-130 - Aggregate Skeleton 1.0 Freeze

## Status

Complete.

## Scope

No new runtime behavior is introduced.

This release freezes the aggregate/ownership learning path after domain ownership, persistence boundaries, and concurrent snapshot protection are all explicit.

## Completed pressures

~~~text
root owns child mutation
    ↓
persistence follows aggregate boundary
    ↓
concurrent stale snapshots
    ↓
root-level optimistic version
~~~

## Final invariant

~~~text
sum(allocation.amount)
<=
project.totalBudget
~~~

Only Project may perform child mutation that can affect this invariant.

## Final concurrency rule

~~~text
one loaded aggregate version
-> at most one successful save
~~~

A stale transaction fails before replacing child rows.

## Why freeze now?

The module can answer the questions it was created to teach:

- why is an aggregate more than a class containing a list?
- why should child mutation go through the root?
- why should repository APIs follow aggregate boundaries?
- why can multiple DB tables still represent one aggregate?
- how can two valid local mutations create a lost update?
- why does the optimistic version belong on the root?
- what exactly does that version protect?

Adding events, sagas, retries, leases, or distributed coordination would introduce new dominant problems.

## Final distinction

~~~text
domain invariant
-> validity of one business mutation

optimistic version
-> freshness of one persisted aggregate snapshot
~~~

## Next pressure

~~~text
07-workflow

One business operation
spans several steps and dependencies.

Who owns the orchestration,
workflow state,
failure handling,
and compensation?
~~~
