# ADR-003 - Protect Order transitions with optimistic locking

## Status

Accepted

## Context

Orders are persisted in PostgreSQL and may be updated by multiple application instances.

Two requests can load the same Order state and both perform individually valid domain transitions before either transaction commits.

Without concurrency control, the last database write wins and one successful business action can disappear.

## Decision

Use optimistic locking with a numeric version column mapped by JPA `@Version`.

Place the transaction boundary at the application use case so the entity version loaded at the start of the command remains the version used when the update is flushed.

Translate optimistic locking failures into `ConcurrentOrderModificationException`.

## Consequences

### Positive

- Lost updates are detected.
- No row lock is held while normal business code executes.
- Independent Orders do not block each other.
- Conflict semantics are explicit at the HTTP boundary.

### Negative

- One racing request fails and the caller must decide whether retry is appropriate.
- The application layer now uses Spring's transaction annotation.
- Correctness depends on load and save staying inside one transaction.

## Alternatives considered

### Pessimistic locking

Not selected because contention is expected to be rare and holding locks earlier would reduce concurrency.

### Serializable isolation

Not selected because it is broader and more expensive than the row-level stale-write problem we need to solve.

### Java synchronized / local lock

Rejected because it protects only one JVM and would fail once multiple application instances run.

### Last-write-wins

Rejected because silently losing a valid lifecycle action is not acceptable.
