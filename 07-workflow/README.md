# 07 - Workflow / Orchestration

## Status

**0.1.0-SNAPSHOT - Explicit Workflow Ownership**

This focused skeleton studies multi-step business operations, partial failure, compensation, and resumability.

## Dominant question

> When one business operation spans several independent steps, who owns sequencing, failure handling, and recovery?

## Current workflow

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

## Current design

~~~text
FulfillmentWorkflow
    |
    +-- InventoryReservation
    +-- PaymentCharging
    +-- ShipmentCreation
~~~

Each capability owns its local operation.

`FulfillmentWorkflow` owns the order.

## Happy path

~~~text
reserve
→ charge
→ ship
→ FulfillmentResult
~~~

The result contains the IDs returned by each step.

## Failure behavior

Forward execution stops immediately on failure.

~~~text
inventory fails
-> payment not called
-> shipment not called
~~~

and:

~~~text
payment fails
-> shipment not called
~~~

## Current limitation

If inventory reservation succeeds and payment later fails:

~~~text
reservation still exists
~~~

The orchestrator can stop future work, but it cannot magically roll back an already completed remote/local side effect.

That distinction creates the next pressure.

## Key lesson

~~~text
step implementation
!=
workflow ownership
~~~

and:

~~~text
stop forward progress
!=
compensate completed work
~~~

## Planned evolution

~~~text
WFL-101  explicit orchestrator owns step order        <- current
WFL-110  partial failure + compensation
WFL-120  persist workflow state / resume after crash
WFL-130  idempotent retry boundary
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

WFL-110 will add compensation in reverse order of completed side effects.
