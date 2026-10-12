# 07 - Workflow / Orchestration

## Status

**0.3.0-SNAPSHOT - Durable Workflow Progress**

This focused skeleton studies multi-step business operations, partial failure, compensation, and resumability.

## Dominant question

> When one business operation spans several independent steps, who owns sequencing, failure handling, and recovery?

## WFL-101 - Explicit orchestration

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

FulfillmentWorkflow owns the order.

Each capability owns only its local operation.

## WFL-110 - Explicit compensation

~~~text
payment fails
-> release inventory

shipment fails
-> refund payment
-> release inventory
~~~

Compensation is an inverse business action, not database rollback.

## WFL-120 - Durable progress

Workflow progress now survives process restart:

~~~text
STARTED
    ↓
INVENTORY_RESERVED
    ↓
PAYMENT_CHARGED
    ↓
COMPLETED
~~~

The database also stores the IDs returned by completed steps.

## Resume

If a process crashes after the PAYMENT_CHARGED checkpoint:

~~~text
new process
→ load PAYMENT_CHARGED
→ create shipment only
~~~

Already checkpointed inventory/payment operations are skipped.

A COMPLETED workflow returns its stored result without repeating side effects.

## No long transaction

There is deliberately no database transaction spanning remote calls.

~~~text
remote side effect
→ DB checkpoint
→ remote side effect
→ DB checkpoint
~~~

A local transaction cannot provide atomicity across independent external systems.

## Remaining crash window

The critical unsolved case is:

~~~text
remote side effect SUCCESS
process CRASH
before checkpoint
~~~

The durable phase is now stale.

A resume may repeat that remote call.

Therefore:

~~~text
durable state
!=
safe replay
~~~

This is the exact pressure for WFL-130.

## Evolution

~~~text
WFL-101  explicit orchestrator owns step order
WFL-110  partial failure + compensation
WFL-120  persist workflow state / resume after crash   <- current
WFL-130  idempotent retry boundary
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

WFL-130 will make external step calls idempotent using stable per-workflow operation keys so replay across the side-effect/checkpoint gap cannot duplicate the business action.
