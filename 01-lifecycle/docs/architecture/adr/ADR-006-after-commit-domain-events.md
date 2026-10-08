# ADR-006 - Record domain events in Order and publish them after commit

## Status

Accepted

## Context

Lifecycle transitions are valid business facts that future application components may need to react to.

Publishing directly from controllers or reconstructing facts from status values would move lifecycle knowledge outside the domain.

Publishing before transaction commit would allow listeners to react to changes that may still roll back.

## Decision

Order records named domain events when valid transitions occur.

OrderService releases those events only after Order persistence succeeds.

The same events are used to append audit history and are scheduled for in-process publication after the surrounding transaction commits.

## Consequences

### Positive

- Event meaning is owned by the domain.
- Invalid transitions record no event.
- Restoring persisted state records no event.
- Audit and publication derive from the same business fact.
- Listeners observe only committed changes.

### Negative

- Order temporarily holds pending events.
- In-process publication is not durable.
- A process crash after database commit can lose an event.
- Listener failures after commit cannot roll the database state back.

## Alternatives considered

### Publish from Order directly

Rejected because the domain would need infrastructure access.

### Publish before commit

Rejected because listeners could react to state that later rolls back.

### Derive events by comparing database state later

Rejected because business intent becomes implicit and reconstruction can lose semantic meaning.

### Add Kafka immediately

Deferred. Kafka does not solve the atomic database-write/message-publish problem by itself.

### Transactional outbox

Selected as the next evolution step once the event model is explicit.
