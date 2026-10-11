# 03 - Scarce Resource / Inventory

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton studies one dominant backend problem:

> How do we prevent overselling when many requests try to consume the same limited stock concurrently?

Unlike `01-lifecycle`, this module deliberately stops once the quantity/concurrency problem is understood.

## Final learning path

~~~text
INV-101  Core quantity model
    ↓
INV-110  Read-check-update race
    ↓
INV-120  Atomic conditional decrement
    ↓
INV-130  High-contention verification
    ↓
1.0      Freeze
~~~

## 1. Quantity invariant

The first local rule is:

~~~text
availableQuantity >= 0
~~~

A single `InventoryItem` owns:

~~~text
take(quantity)
restock(quantity)
~~~

and rejects a take larger than the available stock.

Example:

~~~text
available = 5
take 3
=> available = 2
~~~

If demand exceeds stock:

~~~text
available = 2
take 3
=> reject
=> available remains 2
~~~

## 2. Why a correct domain rule is not enough

The first persistence design was:

~~~text
BEGIN

SELECT available_quantity
    ↓
load InventoryItem
    ↓
item.take(quantity)
    ↓
UPDATE available_quantity

COMMIT
~~~

Sequentially, this is correct.

Under READ COMMITTED, two transactions can still observe the same stale quantity:

~~~text
available = 1

Tx A reads 1
Tx B reads 1

A take(1) -> valid
B take(1) -> valid

A writes 0
B writes 0
~~~

Both service calls succeed.

Final database state:

~~~text
available = 0
~~~

That row is perfectly valid.

But the business history is not:

~~~text
initial stock       = 1
successful consumes = 2
remaining stock     = 0
~~~

This is overselling through a lost update.

## 3. Row validity vs allocation correctness

The schema contains:

~~~sql
CHECK (available_quantity >= 0)
~~~

That protects a row-level state invariant.

It does not prove:

~~~text
every successful consume
corresponded to real stock
~~~

So the important lesson is:

~~~text
valid final row
!=
valid concurrent business outcome
~~~

## 4. Atomic conditional decrement

The safe consumption path becomes one PostgreSQL statement:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - ?
WHERE sku = ?
  AND available_quantity >= ?
RETURNING sku, available_quantity
~~~

The availability decision and mutation happen together.

Interpretation:

~~~text
row returned
    -> stock was consumed

no row returned
    -> no stock was consumed
~~~

A later lookup may classify the failure as:

~~~text
missing SKU
or
insufficient inventory
~~~

but that lookup is not part of the allocation decision.

## 5. Why atomic SQL fits this problem

For the rule:

~~~text
decrement only if current stock >= requested quantity
~~~

the database can express the invariant directly.

That avoids:

~~~text
READ
    ↓
application decision from stale state
    ↓
WRITE
~~~

The domain model still explains the business rule.

The atomic SQL protects the shared-state race.

## 6. High-contention verification

The final test uses:

~~~text
initial stock = 10
consumers = 50
each requests = 1
~~~

All consumers are released from a common start gate.

Required result:

~~~text
10 SUCCESS
40 INSUFFICIENT
final stock = 0
~~~

The stronger invariant is:

~~~text
successful quantity consumed
=
initial stock - final stock
~~~

For unit requests:

~~~text
successful request count
=
initial stock - final stock
~~~

Every externally visible success must correspond to stock that actually disappeared.

## 7. Atomic SQL does not eliminate contention

A hot SKU remains a hot database row.

Many concurrent updates still serialize around shared row state.

Atomic SQL gives:

- correctness
- a short critical operation
- no stale application-side allocation decision
- no ordinary retry loop for insufficient stock

It does not give infinite throughput.

## 8. Strategy selection

The useful rule is not “always use one lock style.”

It is:

~~~text
simple numeric invariant
    -> atomic conditional UPDATE

richer aggregate + rare conflicts
    -> optimistic locking

multi-step decision requiring stable locked state
    -> pessimistic locking / SELECT FOR UPDATE
~~~

### Atomic conditional UPDATE

Best when the decision can be expressed directly in one mutation.

Advantages:

- one correctness statement
- small critical section
- no stale read
- no explicit lock protocol

Trade-off:

- part of the business invariant is expressed in persistence SQL
- hot rows still contend

### Optimistic locking

Best when richer domain logic needs a loaded aggregate and conflicts are relatively rare.

Typical shape:

~~~text
SELECT state + version
calculate
UPDATE ... WHERE version = oldVersion
~~~

Trade-off under heavy contention:

- frequent conflicts
- retry/reload cost
- wasted application work

### Pessimistic locking

Best when multiple decisions must happen against one stable locked state.

Typical shape:

~~~text
SELECT ... FOR UPDATE
check
mutate
UPDATE
~~~

Trade-offs:

- longer critical section
- blocking
- higher deadlock risk when several rows participate

## Final architecture

~~~text
InventoryService
      |
      v
InventoryRepository
      |
      | takeIfAvailable()
      v
PostgreSQL
      |
      +-- atomic conditional decrement
      +-- CHECK available_quantity >= 0
~~~

## What this skeleton teaches

- finite quantity modeling
- domain invariants
- READ COMMITTED lost updates
- overselling despite a valid final row
- deterministic concurrency testing
- row-state correctness vs allocation-history correctness
- atomic conditional SQL
- hot-row contention
- optimistic vs pessimistic vs atomic-update trade-offs
- choosing the smallest concurrency mechanism that fits the invariant

## Deliberately not included

This module does not add:

- HTTP API
- Kafka
- Redis
- inventory reservation lifecycle
- warehouse/location model
- event-driven stock projections
- sharded hot-SKU counters
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

**04 - Assignment / Matching**

Dominant question:

> When several workers/resources are eligible for the same job, how do we select and claim exactly one without duplicate assignment?
