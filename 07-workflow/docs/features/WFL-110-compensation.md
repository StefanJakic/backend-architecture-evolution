# WFL-110 - Compensate Completed Side Effects

## Pressure

WFL-101 made the forward workflow explicit:

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

It also exposed partial success.

If payment fails after inventory reservation, stopping the workflow prevents shipment but does not undo the reservation.

## Compensation is a business action

Compensation is not a database rollback.

It is an explicit inverse operation:

~~~text
reserve inventory
<-> release inventory

charge payment
<-> refund payment
~~~

The inverse action may itself call another service, fail independently, or have different business semantics.

## Reverse order

Completed work is compensated in reverse order.

If shipment fails:

~~~text
reserve
charge
ship -> FAIL
~~~

then:

~~~text
refund payment
release inventory
~~~

Why reverse order?

Because later steps may depend on resources created by earlier steps.

Undo the most recent completed effect first.

## Payment failure

~~~text
reserve -> success
charge  -> FAIL
~~~

Compensation:

~~~text
release reservation
~~~

Shipping never ran, so there is nothing to compensate there.

## Inventory failure

~~~text
reserve -> FAIL
~~~

No side effect completed.

Therefore no compensation runs.

## Original failure stays primary

Suppose shipment creation fails and both compensations also fail.

We do not replace:

~~~text
shipment unavailable
~~~

with:

~~~text
refund failed
~~~

The shipment failure is still the reason compensation began.

Compensation failures are attached as suppressed exceptions.

This preserves:

- primary business failure
- evidence that cleanup was incomplete
- deterministic compensation order

## Why continue after one compensation fails?

If refund fails, inventory release is still attempted.

~~~text
refund -> FAIL
release -> still try
~~~

A failed compensation should not automatically prevent independent remaining cleanup.

## Current limitation

The workflow state lives only in process memory.

If the process crashes after:

~~~text
reserve success
charge success
~~~

but before shipment or compensation completes, a restarted process does not know what already happened.

That is the next pressure.

## Next

WFL-120 will persist workflow progress so execution can resume from durable state rather than starting blindly from step one.
