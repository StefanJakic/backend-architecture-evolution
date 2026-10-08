# ADR-005 - Keep lifecycle audit in the Order transaction

## Status

Accepted

## Context

The service now needs durable history in addition to the current Order state.

Writing the Order and audit record independently could create inconsistent outcomes:

```text
Order = SHIPPED
audit entry missing
```

or:

```text
Order update rolled back
audit says SHIP succeeded
```

## Decision

Append lifecycle audit entries from the application service inside the same PostgreSQL transaction as the Order state change.

Keep audit persistence behind `OrderAuditRepository`.

Use append-only INSERT operations from application code and expose history through a read-only query.

## Consequences

### Positive

- Current state and history commit atomically.
- Failed domain transitions create no audit entry.
- Optimistic-lock losers create no audit entry.
- Idempotent create retries do not duplicate CREATE history.
- Audit storage stays outside the domain model.

### Negative

- Lifecycle use cases now coordinate an additional persistence concern.
- The local transaction couples audit availability to successful writes.
- Audit storage grows continuously and will eventually need retention or archival policy.

## Alternatives considered

### Write audit asynchronously

Rejected for this stage because it creates a window where current state is committed but audit history is missing.

### Put audit logic inside Order

Rejected because persistence, timestamps, and history querying are not lifecycle invariants.

### Reuse application logs

Rejected because logs are operational telemetry, not durable business history.

### Introduce domain events now

Deferred. Domain events are the next evolution step and should be introduced for event semantics, not merely as a hidden way to write an audit row.
