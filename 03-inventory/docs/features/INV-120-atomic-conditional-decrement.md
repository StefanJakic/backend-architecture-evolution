# INV-120 - Atomic Conditional Decrement

## Problem from INV-110

The old write path was:

~~~text
SELECT available
    ↓
check / mutate in Java
    ↓
UPDATE absolute value
~~~

With one unit, two transactions could both read one and both successfully write zero.

The final row looked valid while the system had admitted two consumers.

## New write path

Inventory consumption is now one PostgreSQL statement:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - ?
WHERE sku = ?
  AND available_quantity >= ?
RETURNING sku, available_quantity
~~~

The availability predicate and decrement are one database operation.

## Result interpretation

~~~text
UPDATE returns a row
    -> this request consumed stock

UPDATE returns no row
    -> this request did not consume stock
~~~

Only after a failed mutation does the service perform a normal lookup to classify the failure:

~~~text
no SKU
    -> InventoryNotFoundException

SKU exists
    -> InsufficientInventoryException
~~~

That lookup does not decide whether stock may be consumed.

## Concurrent behavior

Start:

~~~text
available = 1
~~~

Two consumers reach the same atomic statement:

~~~text
A: UPDATE ... WHERE available >= 1
B: UPDATE ... WHERE available >= 1
~~~

One transaction acquires the row for update and decrements:

~~~text
1 -> 0
~~~

The competing transaction can no longer satisfy:

~~~text
available_quantity >= 1
~~~

so it updates no row.

Final result:

~~~text
1 SUCCESS
1 INSUFFICIENT
stored quantity = 0
~~~

One unit now produces exactly one successful consumer.

## Why this is different from a CHECK constraint

~~~sql
CHECK (available_quantity >= 0)
~~~

protects the value stored in one row.

The conditional UPDATE protects the **admission decision**:

> decrement only if the current database row still has enough stock.

Both are useful, but they solve different problems.

## Why not load InventoryItem first?

Loading first creates a stale-state window:

~~~text
READ
... time passes ...
WRITE
~~~

For a hot counter, the business decision can often be expressed directly in SQL and made atomic with the mutation.

The domain model still documents local quantity semantics and remains useful for non-concurrent reasoning.

## Why not optimistic locking?

A version column could detect that another transaction changed the row.

That would normally require:

~~~text
read
attempt update with version
conflict
reload
retry or reject
~~~

That is a valid design when richer domain computation must happen before writing.

For a simple counter invariant, conditional decrement expresses the rule more directly.

## Why not SELECT FOR UPDATE?

Pessimistic locking could serialize:

~~~text
SELECT ... FOR UPDATE
check
UPDATE
~~~

That is also correct.

It uses more statements and keeps the lock while application code decides.

For a small numeric invariant, one conditional UPDATE is simpler.

## Important remaining limitation

`restock()` still uses the original load/mutate/update path.

This skeleton currently guarantees concurrent safety for the pressure it is studying:

~~~text
multiple consumers competing for limited stock
~~~

INV-130 will use high-contention tests to make the trade-offs and remaining write shapes explicit.
