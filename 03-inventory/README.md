# 03 - Scarce Resource / Inventory

## Status

**0.1.0-SNAPSHOT - Core Quantity Model**

This focused skeleton studies quantity contention and overselling.

## Dominant question

> How do we prevent overselling when many requests try to consume the same limited stock concurrently?

## Core invariant

~~~text
availableQuantity >= 0
~~~

Current domain behavior:

~~~text
available = 5
take 3
=> 2

available = 2
take 3
=> reject
~~~

## Current scope

- SKU identity
- available quantity
- take
- restock
- insufficient-inventory rejection
- pure Java + JUnit

No Spring or database yet.

## Planned evolution

~~~text
INV-101  Core quantity model
INV-110  PostgreSQL read-check-update race
INV-120  Atomic conditional decrement
INV-130  High-contention strategy comparison
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

The domain rule is correct inside one object.

INV-110 asks what happens when two database transactions both read the same available quantity before either writes its result.
