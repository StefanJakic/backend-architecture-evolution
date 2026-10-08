# ORD-170 - Domain Events

## Problem

The service now has:

- current Order state
- durable lifecycle audit history

but it still does not model business facts as first-class objects.

Code can observe that an Order is now CONFIRMED, but there is no explicit object representing:

```text
OrderConfirmed
```

## Domain decision

The Order aggregate now records events when valid lifecycle changes happen:

```text
new Order      -> OrderCreated
confirm()      -> OrderConfirmed
ship()         -> OrderShipped
complete()     -> OrderCompleted
cancel()       -> OrderCancelled
```

The domain decides which fact occurred because the domain already owns transition validity.

## Restore is different from create

Loading a persisted Order is not a new business event.

Therefore:

```java
new Order(id)
```

records `OrderCreated`, while:

```java
Order.restore(id, status)
```

records nothing.

This distinction prevents every database read from inventing historical events.

## Pending events

Order keeps newly created facts temporarily until the application layer persists the state:

```text
Order.confirm()
    ↓
status = CONFIRMED
pending event = OrderConfirmed
```

After persistence succeeds, `OrderService` releases those events.

## One fact source

The released event is used for two purposes:

```text
OrderConfirmed
    ├── append durable audit row
    └── schedule in-process publication after commit
```

This removes the need for OrderService to separately reconstruct `CREATED -> CONFIRMED` from outside the domain.

## After-commit publication

Listeners should not react to a transition that later rolls back.

The current publisher therefore registers an after-commit callback:

```text
BEGIN
  domain transition
  persist Order
  append audit
COMMIT
  ↓
publish domain event in process
```

An invalid transition, persistence failure, or optimistic-lock conflict never reaches event publication.

## Why this is not reliable messaging yet

There is still a failure window:

```text
database COMMIT succeeds
        ↓
process crashes
        ↓
after-commit event is never published
```

The database contains the new Order state and audit row, but another component may never observe the event.

That is not a bug hidden by this feature. It is the exact reliability problem the next feature will solve.

## Domain event vs audit

Audit:

> What committed history should this service retain?

Domain event:

> What business fact just occurred that another part of the system may react to?

Today the same fact feeds both, but they have different responsibilities.

## Domain event vs Kafka event

`OrderConfirmed` is currently a Java domain object.

It is not yet a Kafka schema or external integration contract.

The next steps will decide how a domain event becomes a durable outbox record and, later, an external message.

## Next pressure

**ORD-180 - Transactional Outbox**

Persist the event in the same transaction as the Order so a process crash after commit cannot silently lose it.
