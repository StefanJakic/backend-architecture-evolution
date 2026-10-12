# 07 - Workflow / Orchestration

## Status

**0.2.0-SNAPSHOT - Explicit Compensation**

This focused skeleton studies multi-step business operations, partial failure, compensation, and resumability.

## Dominant question

> When one business operation spans several independent steps, who owns sequencing, failure handling, and recovery?

## Forward workflow

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

## WFL-101 - One owner of forward progress

~~~text
FulfillmentWorkflow
    |
    +-- InventoryReservation
    +-- PaymentCharging
    +-- ShipmentCreation
~~~

The workflow decides what happens next.

A step owns only how its own capability works.

## WFL-110 - Completed side effects need compensation

Forward and inverse operations:

~~~text
reserve <-> release
charge  <-> refund
~~~

Failure after payment:

~~~text
reserve
charge
ship -> FAIL
    ↓
refund
release
~~~

Compensation runs in reverse order of completed side effects.

## Failure matrix

~~~text
reserve fails
-> no compensation

charge fails
-> release inventory

shipment fails
-> refund payment
-> release inventory
~~~

## Compensation failure

The original forward failure remains the primary exception.

Compensation failures are preserved as suppressed exceptions.

Example:

~~~text
shipment FAIL
refund FAIL
release FAIL

primary:
shipment failure

suppressed:
refund failure
release failure
~~~

Remaining compensations are still attempted after one compensation fails.

## Key lesson

~~~text
exception propagation
!=
distributed rollback
~~~

and:

~~~text
compensation
=
explicit inverse business action
~~~

## Current limitation

The workflow knows what completed only while the process is alive.

A crash can erase that knowledge.

## Evolution

~~~text
WFL-101  explicit orchestrator owns step order
WFL-110  partial failure + compensation             <- current
WFL-120  persist workflow state / resume after crash
WFL-130  idempotent retry boundary
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

WFL-120 will make completed-step state durable so a restarted process can decide whether to continue forward or compensate.
