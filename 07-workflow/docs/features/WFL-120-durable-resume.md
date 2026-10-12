# WFL-120 - Persist Workflow Progress and Resume After Crash

## Pressure

WFL-110 can compensate failures while the process is alive.

But process memory is not durable.

If the process disappears after completed steps, a new process needs to know what already happened.

## Durable phases

The workflow now records:

~~~text
STARTED
    ↓
INVENTORY_RESERVED(reservationId)
    ↓
PAYMENT_CHARGED(paymentId)
    ↓
COMPLETED(shipmentId)
~~~

Failure cleanup also records:

~~~text
COMPENSATED
COMPENSATION_FAILED
~~~

## Start vs resume

A new command creates durable STARTED state if the order does not already have a workflow.

Then execution always proceeds from persisted state.

~~~text
fulfill(command)
    ↓
createIfAbsent
    ↓
load durable phase
    ↓
resume from that phase
~~~

If a completed workflow is called again with the same order and amount, the stored result is returned without repeating side effects.

## Restart example

Suppose the first process reaches:

~~~text
reserve SUCCESS
checkpoint INVENTORY_RESERVED

charge SUCCESS
checkpoint PAYMENT_CHARGED

process CRASH
~~~

The database still contains:

~~~text
phase = PAYMENT_CHARGED
reservation_id = ...
payment_id = ...
~~~

A restarted process calls:

~~~text
resume(orderId)
~~~

and continues directly with:

~~~text
create shipment
~~~

Inventory and payment are not repeated.

## Why no transaction around the whole workflow?

The workflow intentionally does not do:

~~~text
BEGIN DB TRANSACTION
reserve remote inventory
charge remote payment
create remote shipment
COMMIT
~~~

A local database transaction cannot make independent remote side effects atomic.

Holding it open across network calls would also create long locks and poor failure behavior.

Instead:

~~~text
side effect
→ durable checkpoint

side effect
→ durable checkpoint
~~~

Each checkpoint is small.

## The important remaining gap

Durable state solves one question:

> What step was last durably recorded?

It does **not** solve this crash window:

~~~text
charge payment SUCCESS
        ↓
PROCESS CRASH
        ↓
before PAYMENT_CHARGED checkpoint
~~~

The database still says:

~~~text
INVENTORY_RESERVED
~~~

After restart, the workflow will attempt payment again.

The same ambiguity exists for:

- reserve -> inventory checkpoint
- payment -> payment checkpoint
- shipment -> completion checkpoint
- compensation side effect -> compensation checkpoint

## Why not hide this in WFL-120?

Because persistence and idempotency solve different problems.

~~~text
durable workflow state
-> remembers acknowledged progress

idempotent side-effect boundary
-> makes replay safe when acknowledgement is uncertain
~~~

Mixing both changes at once would hide the reason each mechanism exists.

## Single-runner assumption

WFL-120 also assumes one active runner per order.

Two concurrent runners could both observe the same phase and perform the same side effect before either checkpoint wins.

WFL-130 will make repeated step invocation safe at the external boundary.

## Compensation state

If forward execution fails and compensation completes:

~~~text
COMPENSATED
~~~

If at least one compensation fails:

~~~text
COMPENSATION_FAILED
~~~

Those terminal states are not blindly resumed as forward fulfillment.

## Next pressure

WFL-130 will derive stable operation keys from the workflow identity and step so retried external actions return the original result instead of duplicating side effects.
