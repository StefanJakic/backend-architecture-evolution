# 03 - Scarce Resource / Inventory

## Status

**0.3.0-SNAPSHOT - Atomic Conditional Decrement**

This focused skeleton studies quantity contention and overselling.

## Dominant question

> How do we prevent overselling when many requests try to consume the same limited stock concurrently?

## Evolution so far

### INV-101 - Quantity model

The local invariant is:

~~~text
availableQuantity >= 0
~~~

and a single `InventoryItem` rejects taking more than it owns.

### INV-110 - Lost update

Persistence introduced:

~~~text
SELECT
domain take
UPDATE
~~~

With one unit:

~~~text
A reads 1
B reads 1

A writes 0
B writes 0

=> both requests succeed
~~~

The row remains non-negative, but the business history is invalid.

### INV-120 - Atomic conditional decrement

Consumption is now:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - ?
WHERE sku = ?
  AND available_quantity >= ?
RETURNING sku, available_quantity
~~~

So:

~~~text
stock = 1
two concurrent take(1)

=> 1 success
=> 1 insufficient
=> stock = 0
~~~

## Key distinction

~~~text
domain invariant
    -> what a valid quantity decision means

database mutation
    -> who wins when many transactions make that decision concurrently
~~~

For this hot counter, the predicate and mutation belong together.

## Current architecture

~~~text
InventoryService
      |
      | take()
      v
InventoryRepository
      |
      | atomic conditional UPDATE
      v
PostgreSQL
      |
      +-- CHECK available_quantity >= 0
~~~

Failed atomic take:

~~~text
0 rows returned
    ↓
lookup only for error classification
    ├─ missing SKU
    └─ insufficient stock
~~~

The lookup does not participate in the allocation decision.

## Why not optimistic/pessimistic locking?

Both can be valid.

For the simple rule:

~~~text
decrement only if current quantity >= requested quantity
~~~

one conditional SQL statement is smaller and avoids an application-level retry/lock protocol.

## Evolution

~~~text
INV-101  Core quantity model
INV-110  PostgreSQL read-check-update race
INV-120  Atomic conditional decrement          <- current
INV-130  High-contention strategy comparison
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

INV-130 will put many concurrent consumers against a small stock and verify the stronger invariant:

~~~text
number of successful takes
=
initial stock - final stock
~~~

Then we will compare where atomic SQL, optimistic locking, and pessimistic locking fit rather than adding all three implementations just for pattern coverage.
