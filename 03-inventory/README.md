# 03 - Scarce Resource / Inventory

## Status

**0.4.0-SNAPSHOT - High-Contention Verification**

This focused skeleton studies quantity contention and overselling.

## Dominant question

> How do we prevent overselling when many requests try to consume the same limited stock concurrently?

## Learning path

~~~text
INV-101  Quantity invariant
    ↓
INV-110  Lost update / overselling
    ↓
INV-120  Atomic conditional decrement
    ↓
INV-130  High-contention verification
~~~

## 1. Local quantity rule

~~~text
availableQuantity >= 0
~~~

A single domain object rejects taking more stock than it owns.

## 2. Why that is not enough

The original persistence flow:

~~~text
SELECT
take in Java
UPDATE absolute quantity
~~~

allowed:

~~~text
stock = 1

A reads 1
B reads 1

A succeeds
B succeeds

both write 0
~~~

The row looked valid while the business outcome oversold.

## 3. Atomic decrement

Consumption now uses:

~~~sql
UPDATE inventory_item
SET available_quantity = available_quantity - ?
WHERE sku = ?
  AND available_quantity >= ?
RETURNING sku, available_quantity
~~~

The database performs the admission check and mutation together.

## 4. High contention

The final concurrency scenario uses:

~~~text
stock = 10
50 concurrent consumers
each wants 1
~~~

Required result:

~~~text
10 SUCCESS
40 INSUFFICIENT
final stock = 0
~~~

And the stronger invariant:

~~~text
successful takes
=
initial stock - final stock
~~~

## Important lesson

Atomic SQL does not eliminate contention.

All requests for one SKU still compete for the same database row.

What it eliminates is the stale read/check/write protocol for this simple invariant.

## Strategy selection

~~~text
simple numeric invariant
    -> atomic conditional UPDATE

richer aggregate + rare conflicts
    -> optimistic locking

multi-step locked decision
    -> pessimistic locking / SELECT FOR UPDATE
~~~

The goal is not to implement every pattern.

The goal is to choose the smallest mechanism that correctly protects the invariant.

## Current architecture

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

## Deliberate omissions

No:

- HTTP
- Kafka
- Redis
- inventory reservation lifecycle
- warehouse model
- Kubernetes
- AWS
- benchmarking claims

Those are different pressures.

## Evolution

~~~text
INV-101  Core quantity model
INV-110  PostgreSQL read-check-update race
INV-120  Atomic conditional decrement
INV-130  High-contention verification       <- current
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

After this feature is verified, only the **1.0 freeze** remains.
