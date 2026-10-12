# WFL-130 - Idempotent Retry Boundary

## Pressure

WFL-120 made acknowledged workflow progress durable.

It still cannot remove the ambiguity between a successful remote side effect and the following local checkpoint:

~~~text
remote side effect SUCCESS
        ↓
process CRASH
        ↓
before durable checkpoint
~~~

After restart, the database contains the previous workflow phase.

The orchestrator must assume the remote operation may need to be called again.

## Stable operation identity

Every forward side effect now receives a deterministic operation key derived from:

~~~text
workflow identity + logical step
~~~

For order `order-123`:

~~~text
order-123:reserve
order-123:charge
order-123:shipment
~~~

The key does not change across process restarts or retry attempts.

## Why a random retry key is wrong

A new random key on every attempt would make each retry look like a new business operation:

~~~text
attempt 1 -> random-A
attempt 2 -> random-B
~~~

The downstream system could legitimately execute both.

Replay safety requires:

~~~text
attempt 1 -> order-123:charge
attempt 2 -> order-123:charge
~~~

## External boundary contract

The orchestrator does not itself know whether the first remote call committed.

The downstream capability must treat the operation key as an idempotency key:

~~~text
first call
-> execute side effect
-> persist operationKey -> original result
-> return result

same key again
-> do not execute side effect again
-> return original result
~~~

## Regression scenario

The integration test deliberately crashes before the payment checkpoint is stored:

~~~text
reserve SUCCESS
checkpoint INVENTORY_RESERVED

charge(order-X:charge) SUCCESS
    ↓
PROCESS CRASH
    ↓
markPaymentCharged was never committed
~~~

Durable state therefore remains:

~~~text
INVENTORY_RESERVED
~~~

After restart:

~~~text
charge(order-X:charge)
~~~

is invoked again.

The payment test double records:

~~~text
request attempts = 2
logical charges  = 1
~~~

and returns the same payment ID.

The workflow can then checkpoint PAYMENT_CHARGED and continue to shipment.

## Core lesson

~~~text
durable progress
!=
safe replay
~~~

Durable workflow state remembers what was acknowledged locally.

Idempotency makes repetition safe when acknowledgement is uncertain.

Also:

~~~text
at-least-once invocation
+
idempotent business boundary
=
safe retry of the logical operation
~~~

This does not mean the network delivered the request exactly once.

## Scope

WFL-130 focuses on forward workflow side effects:

- inventory reservation
- payment charging
- shipment creation

Existing explicit compensation behavior is preserved.

A production system would apply the same reasoning to compensation operations when they also need crash-safe replay.

## Next

Freeze the workflow skeleton at 1.0.
