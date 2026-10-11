# INV-140 - Inventory Skeleton 1.0 Freeze

## Status

Complete.

## Scope

No new business behavior is introduced in this release.

The purpose of 1.0 is to mark the quantity/concurrency learning objective as complete and prevent the module from growing into unrelated inventory-platform concerns.

## Completed pressures

~~~text
quantity invariant
    ↓
read-check-update lost race
    ↓
overselling with a valid final row
    ↓
atomic conditional decrement
    ↓
high-contention verification
~~~

## Why freeze now?

The skeleton can answer the questions it was created to teach:

- why is `availableQuantity >= 0` necessary but insufficient?
- how can two successful requests consume one unit without the row becoming negative?
- why does `@Transactional` not automatically prevent lost updates?
- when is a single conditional SQL mutation stronger and simpler than load/check/save?
- what exactly does a database CHECK constraint protect?
- why does a hot SKU still contend even after correctness is fixed?
- when should optimistic or pessimistic locking be preferred instead?

Adding warehouses, reservation holds, events, caches, messaging, or deployment would introduce new dominant problems rather than deepen this one.

## Final accounting invariant

The strongest test in the module is:

~~~text
successful quantity consumed
=
initial stock - final stock
~~~

That connects API-visible success to real shared-state mutation.

## Strategy lesson

~~~text
simple numeric invariant
    -> atomic conditional UPDATE

rich aggregate + rare conflicts
    -> optimistic locking

multi-step locked decision
    -> pessimistic locking
~~~

The goal is selection judgment, not implementation of every concurrency pattern.

## Next pressure

The next skeleton moves from quantity to selection:

~~~text
04-assignment

job needs one worker
several workers are eligible
several schedulers may race

How do we choose and claim exactly one?
~~~
