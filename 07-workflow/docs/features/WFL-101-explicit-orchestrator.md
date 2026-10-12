# WFL-101 - Make Workflow Step Order Explicit

## Goal

Model one business operation that spans several independent capabilities.

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

The first architecture pressure is not compensation or messaging.

It is ownership of the sequence itself.

## Why an orchestrator?

Without one workflow owner, sequencing tends to leak into step implementations:

~~~text
inventory calls payment
payment calls shipping
~~~

That couples each capability to what happens after it.

Instead:

~~~text
FulfillmentWorkflow
    |
    +-- InventoryReservation
    +-- PaymentCharging
    +-- ShipmentCreation
~~~

Each step answers:

> How do I perform my capability?

The workflow answers:

> What happens next?

## Explicit order

The happy path is intentionally visible in one method:

~~~text
reservationId = reserveInventory()
paymentId = chargePayment()
shipmentId = createShipment()
~~~

The workflow returns the identifiers produced by each completed step.

## Failure stops forward progress

If inventory reservation fails:

~~~text
inventory -> FAIL

payment  -> not called
shipment -> not called
~~~

If payment fails:

~~~text
inventory -> success
payment   -> FAIL
shipment  -> not called
~~~

The orchestrator owns this forward control flow.

## But stopping is not rollback

The important pressure exposed by WFL-101 is:

~~~text
inventory reservation succeeds
payment fails
~~~

The workflow stops.

But the already completed reservation still exists.

~~~text
stop future steps
!=
undo completed side effects
~~~

That is not a bug in this step.

It is the next architecture problem.

## Why no Saga abstraction yet?

At this stage there are only three visible steps.

A named Saga framework would add structure before we have modeled:

- which completed steps need compensation
- compensation order
- compensation failure
- workflow state

First make the actual control flow explicit.

Then add architecture when failure pressure requires it.

## Important lesson

~~~text
step implementation
!=
workflow ownership
~~~

and:

~~~text
exception propagation
!=
distributed rollback
~~~

## Next pressure

WFL-110 will add explicit compensation.

If payment fails after inventory reservation:

~~~text
release inventory
~~~

If shipment creation fails after payment:

~~~text
refund payment
release inventory
~~~

Compensation will run in reverse order of completed side effects.
