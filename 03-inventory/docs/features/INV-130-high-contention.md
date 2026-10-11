# INV-130 - High-Contention Inventory Verification

## Goal

INV-120 proved the atomic conditional decrement with two competing consumers.

INV-130 increases the pressure:

~~~text
initial stock = 10
consumers = 50
each wants 1
~~~

Expected result:

~~~text
10 SUCCESS
40 INSUFFICIENT
final stock = 0
~~~

## Stronger invariant

The important assertion is not only:

~~~text
final stock >= 0
~~~

It is:

~~~text
successful quantity consumed
=
initial stock - final stock
~~~

For unit-size requests:

~~~text
successful request count
=
initial stock - final stock
~~~

That ties externally visible success directly to real inventory movement.

## Common start gate

The test prepares all consumers first:

~~~text
50 threads
    ↓
ready barrier
    ↓
release together
~~~

This creates real contention without relying on random timing.

Unlike INV-110, we do not intercept repository reads because there is no separate read in the correctness path anymore.

## What happens in PostgreSQL

All consumers execute:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - 1
WHERE sku = ?
  AND available_quantity >= 1
RETURNING sku, available_quantity
~~~

They compete for the same row.

PostgreSQL still serializes conflicting writes to that row.

The important difference is what happens after each winner commits:

~~~text
10 -> 9
9  -> 8
...
1  -> 0
~~~

After stock reaches zero, later statements no longer satisfy:

~~~text
available_quantity >= 1
~~~

and therefore update no row.

## Atomic SQL does not remove contention

A hot SKU is still a hot row.

If thousands of requests all mutate the same row, throughput is bounded by contention on that shared state.

The benefit of the atomic statement is correctness and simplicity, not infinite scalability.

## Strategy comparison

### 1. Atomic conditional UPDATE

Shape:

~~~text
UPDATE ... WHERE available >= requested
~~~

Best fit when:

- the invariant can be expressed directly in SQL
- mutation is small
- one row owns the counter
- callers only need success/failure plus the new value

Advantages:

- one correctness statement
- no stale application read
- no retry loop for ordinary insufficient stock
- no explicit lock API
- easy to reason about under contention

Trade-off:

- business logic moves partly into persistence SQL
- still serializes on one hot row

### 2. Optimistic locking

Shape:

~~~text
SELECT quantity, version
calculate
UPDATE ... WHERE version = oldVersion
~~~

Best fit when:

- richer domain computation needs a loaded aggregate
- conflicts are relatively uncommon
- retry/reject behavior is acceptable

Advantages:

- keeps more business computation in application/domain code
- detects stale writes

Trade-offs under a hot counter:

- many conflicts
- retries increase work
- a heavily contended SKU can become retry-heavy

### 3. Pessimistic locking / SELECT FOR UPDATE

Shape:

~~~text
SELECT ... FOR UPDATE
check in application
UPDATE
~~~

Best fit when:

- multiple related decisions must happen while holding one stable snapshot
- application logic cannot be expressed safely in one statement
- waiting is preferable to retrying

Advantages:

- very explicit serialization
- easy to reason about for multi-step row logic

Trade-offs:

- lock held across multiple statements/application work
- longer critical section
- greater blocking/deadlock risk when multiple rows are involved

## Why this skeleton keeps only atomic decrement

Implementing all three would add code without adding a new dominant lesson.

The selection rule is more useful:

~~~text
simple counter invariant
    -> atomic conditional UPDATE

rich aggregate + rare conflicts
    -> optimistic locking

multi-step decision that must see locked state
    -> pessimistic locking
~~~

## Remaining production-scale questions

Outside this skeleton:

- partitioning inventory across warehouses
- per-location stock
- inventory reservation / release
- event-driven stock projection
- very hot SKU sharding
- queueing/admission control
- fairness
- backpressure

Those are different architecture pressures.

## Result

The inventory learning path is now functionally complete:

~~~text
quantity invariant
    ↓
lost update / overselling
    ↓
atomic conditional decrement
    ↓
high-contention verification
~~~

After local verification, only the 1.0 freeze remains.
