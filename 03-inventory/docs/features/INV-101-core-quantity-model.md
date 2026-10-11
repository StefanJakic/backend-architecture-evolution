# INV-101 - Core Quantity Model

## Dominant problem

Inventory is about a finite quantity shared by many consumers.

The first invariant is simple:

~~~text
availableQuantity >= 0
~~~

Before persistence or locks exist, one domain object should make an invalid local state impossible.

## Operations

~~~text
take(quantity)
restock(quantity)
~~~

Both require a positive quantity.

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
=> InsufficientInventoryException
=> available remains 2
~~~

## Why start without a database?

The business rule must be correct before concurrency is introduced.

If this rule were wrong, no locking strategy could save it.

But even when the rule is correct, the next feature will show that two transactions can each make a valid decision from stale/shared data.

## Next pressure

INV-110 moves one SKU into PostgreSQL and deliberately implements:

~~~text
read available quantity
check in application
write new quantity
~~~

With one item left:

~~~text
Tx A reads 1
Tx B reads 1

A decides take(1) is valid
B decides take(1) is valid
~~~

That is the quantity equivalent of the check-then-insert race from reservations, but the shape of the fix will be different.
