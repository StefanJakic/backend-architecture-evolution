# 03 - Scarce Resource / Inventory

## Status

**0.2.0-SNAPSHOT - PostgreSQL Read-Check-Update Race**

This focused skeleton studies quantity contention and overselling.

## Dominant question

> How do we prevent overselling when many requests try to consume the same limited stock concurrently?

## Core invariant

At the domain level:

~~~text
availableQuantity >= 0
~~~

But INV-110 shows that this alone is not enough.

## Current architecture

~~~text
InventoryService
      |
      v
InventoryRepository
      |
      v
PostgreSQL
~~~

The current write path is deliberately naive:

~~~text
SELECT available
    ↓
InventoryItem.take()
    ↓
UPDATE available
~~~

## Sequential behavior

~~~text
available = 5
take 3
=> stored = 2
~~~

and:

~~~text
available = 2
take 3
=> InsufficientInventoryException
=> stored = 2
~~~

## Concurrent bug

With one unit:

~~~text
Tx A READ -> 1
Tx B READ -> 1

A take(1) -> valid
B take(1) -> valid

A UPDATE -> 0
B UPDATE -> 0
~~~

Both calls succeed.

Final row:

~~~text
available = 0
~~~

The database row is non-negative, but the system accepted two consumers for one unit.

That is overselling through a lost update.

## Important distinction

~~~text
stored quantity is valid
!=
allocation history is valid
~~~

A `CHECK (available_quantity >= 0)` cannot detect two transactions that both calculate the same zero from the same stale one.

## Evolution

~~~text
INV-101  Core quantity model
INV-110  PostgreSQL read-check-update race   <- current
INV-120  Atomic conditional decrement
INV-130  High-contention strategy comparison
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

INV-120 will remove the separate read/check/write decision.

PostgreSQL will atomically perform:

~~~text
decrement only if enough quantity still exists
~~~

so one unit can produce only one successful take.
