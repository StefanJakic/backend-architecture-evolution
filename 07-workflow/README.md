# 07 - Workflow / Orchestration

## Status

**0.4.0-SNAPSHOT - Idempotent Replay Boundary**

This focused skeleton studies multi-step business operations, partial failure, compensation, resumability, and safe replay.

## Dominant question

> When one business operation spans several independent steps, who owns sequencing, failure handling, recovery, and replay safety?

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

Workflow progress survives process restart:

~~~text
STARTED
    ↓
INVENTORY_RESERVED
    ↓
PAYMENT_CHARGED
    ↓
COMPLETED
~~~

The database stores the IDs returned by completed steps.

A restarted process loads the durable phase and skips already checkpointed steps.

## WFL-130 - Idempotent replay boundary

Durable state alone cannot answer whether a remote side effect succeeded immediately before a crash:

~~~text
charge payment SUCCESS
process CRASH
before PAYMENT_CHARGED checkpoint
~~~

After restart the durable phase is still INVENTORY_RESERVED, so payment is called again.

Every forward step now receives a deterministic operation key:

~~~text
order-123:reserve
order-123:charge
order-123:shipment
~~~

A retry of the same logical step reuses exactly the same key.

The external boundary can therefore return the original result instead of repeating the business side effect.

## Request attempts vs business side effects

A safe replay can legitimately look like:

~~~text
payment requests = 2
logical charges  = 1
~~~

The orchestrator is allowed to retry an uncertain call.

The downstream boundary owns deduplication for the stable operation key.

## Crash regression

The integration test proves:

~~~text
reserve SUCCESS
checkpoint INVENTORY_RESERVED

charge(order-1:charge) SUCCESS
PROCESS CRASH
before PAYMENT_CHARGED checkpoint

restart
charge(order-1:charge) again
-> same paymentId
-> no second logical charge

checkpoint PAYMENT_CHARGED
create shipment
checkpoint COMPLETED
~~~

## No long transaction

There is deliberately no database transaction spanning remote calls.

~~~text
remote side effect
→ DB checkpoint
→ remote side effect
→ DB checkpoint
~~~

A local transaction cannot provide atomicity across independent external systems.

## Important boundary

Idempotency is not the same as exactly-once transport.

~~~text
request delivery:       may happen more than once
business side effect:   must happen once per operation key
~~~

The external capability must durably remember the operation key and its original result in a real distributed system.

The test double models that contract in memory.

## Evolution

~~~text
WFL-101  explicit orchestrator owns step order
WFL-110  partial failure + compensation
WFL-120  persist workflow state / resume after crash
WFL-130  idempotent retry boundary                    <- current
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

Freeze the workflow skeleton at 1.0 and document the architectural guarantees and remaining production concerns.
