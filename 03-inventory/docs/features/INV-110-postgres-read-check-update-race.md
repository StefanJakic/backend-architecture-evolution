# INV-110 - PostgreSQL Read-Check-Update Race

## Goal

Move inventory into PostgreSQL and show why a correct domain rule can still oversell under concurrency.

## Naive transaction

The service performs:

~~~text
BEGIN

SELECT available_quantity
    ↓
InventoryItem.take(quantity)
    ↓
UPDATE available_quantity

COMMIT
~~~

Sequentially, this is correct.

## Deterministic race

Start with:

~~~text
available = 1
~~~

Then force:

~~~text
Transaction A                       Transaction B

SELECT available -> 1

                                    SELECT available -> 1

[wait]                              [wait]

take(1) -> local quantity 0         take(1) -> local quantity 0

UPDATE quantity = 0                 UPDATE quantity = 0

COMMIT                              COMMIT
~~~

Both service calls succeed.

Final database state:

~~~text
available = 0
~~~

That row looks valid.

But the business outcome is not:

~~~text
initial units       = 1
successful consumes = 2
remaining units     = 0
~~~

The system accepted two sales against one unit.

## Why the database CHECK does not help

The schema contains:

~~~sql
CHECK (available_quantity >= 0)
~~~

Neither transaction writes a negative value.

Both write zero.

The constraint protects a row-level state invariant but does not protect the history of how many successful consumers were admitted.

## Lost update

This is a lost-update shape:

~~~text
A reads 1
B reads 1

A calculates 0
B calculates 0

A writes 0
B writes 0
~~~

The second write overwrites based on stale state.

## Why @Transactional is not enough

Each transaction is atomic by itself.

READ COMMITTED does not mean both transactions read one shared value and reserve unique units from it.

The read and later write are separate operations.

## What INV-120 will change

Instead of:

~~~text
SELECT
check in Java
UPDATE
~~~

the next feature will make the database perform the decision and mutation atomically:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - ?
WHERE sku = ?
  AND available_quantity >= ?
~~~

Then:

~~~text
rows updated = 1
    -> success

rows updated = 0
    -> insufficient inventory
~~~

The quantity predicate and decrement happen in one database statement against the current row state.
