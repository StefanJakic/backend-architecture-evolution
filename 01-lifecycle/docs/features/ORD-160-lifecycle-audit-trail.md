# ORD-160 - Lifecycle Audit Trail

## Problem

The `orders` table stores the current state:

```text
ORDER-42
status = SHIPPED
```

but it cannot answer how the Order reached that state.

For business investigation, support, and debugging we need committed lifecycle history.

## Audit model

Each successful lifecycle change appends a row containing:

```text
order_id
action
from_status
to_status
occurred_at
```

Example:

```text
ORDER-42 | CREATE  | null      | CREATED   | ...
ORDER-42 | CONFIRM | CREATED   | CONFIRMED | ...
ORDER-42 | SHIP    | CONFIRMED | SHIPPED   | ...
```

The current Order row remains the source of truth for current state. The audit table explains the path to that state.

## Transaction boundary

Audit is written by `OrderService` inside the same transaction as the state change:

```text
BEGIN

load Order
capture previous status
execute domain transition
persist Order
append audit entry

COMMIT
```

If persistence or audit insertion fails, both roll back.

This also means an optimistic-lock loser cannot leave an audit row for a transition that never committed.

## Why audit is not inside Order

`Order` owns lifecycle rules:

```text
CREATED -> CONFIRMED
CONFIRMED -> SHIPPED
```

It does not need to know:

- which SQL table stores history
- when a timestamp is generated
- how history is queried
- how the API exposes audit records

Those belong to application and infrastructure concerns.

## Idempotent creation

A successful first `POST /orders` writes one CREATE audit entry.

A retry with the same `Idempotency-Key` returns the existing Order before creation logic runs, so it does not append another CREATE entry.

## Read API

```http
GET /orders/{orderId}/history
```

returns audit entries in append order.

The endpoint is intentionally read-only.

## Audit vs logs

Application logs answer operational questions about process execution and may be rotated or sampled.

The lifecycle audit trail is durable business data tied to an Order.

It should not depend on log retention.

## Audit vs domain events

Audit answers:

> What committed lifecycle changes happened to this Order?

Domain events will later answer:

> What business fact occurred that other parts of the system may react to?

The concepts overlap, but this feature deliberately does not introduce event publication yet.

## Deliberate limitations

The audit currently records lifecycle action, state change, and time.

There is no authenticated actor or user identity in the application yet, so we do not invent one just to populate an audit column.

Actor metadata can be added when authentication introduces a real identity source.
