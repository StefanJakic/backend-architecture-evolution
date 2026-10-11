# ADR-001 - Enforce Reservation Overlap in PostgreSQL

## Status

Accepted

## Context

The application can correctly detect an existing overlapping reservation, but RSV-110 proved that two READ COMMITTED transactions can both pass that check before either INSERT becomes visible.

The invariant is:

> One resource cannot have two overlapping reservation windows.

The conflict spans a set of rows rather than concurrent updates to one row.

## Decision

Use a PostgreSQL GiST exclusion constraint combining:

- equality on `resource_id`
- overlap on a `tstzrange`

Use `btree_gist` to provide GiST equality support for the scalar resource id.

Keep the application overlap query for early validation, but treat the database constraint as authoritative.

Translate exclusion violation SQLSTATE `23P01` into `ReservationConflictException` inside the PostgreSQL adapter.

## Consequences

### Positive

- double booking cannot commit even with multiple application replicas
- the database constraint exactly matches the domain's half-open interval semantics
- adjacent reservations remain valid
- application code receives a domain-level conflict instead of a PostgreSQL-specific exception
- no JVM/distributed lock is required

### Negative

- the solution uses a PostgreSQL-specific feature
- migrations require the `btree_gist` extension
- existing overlapping rows must be cleaned before adding the constraint
- a competing INSERT may wait for the other transaction to resolve before PostgreSQL can decide whether it conflicts

## Alternatives

### Application check only

Rejected because RSV-110 proves the race.

### Optimistic locking on Reservation

Rejected because the competing operations insert different rows.

### Lock a parent Resource row

Valid, but serializes booking attempts for that resource and introduces an extra locking model.

### SERIALIZABLE isolation

Valid, especially for invariants that cannot be represented as a database constraint. It requires transaction-level retry handling for serialization failures.

For this invariant, an exclusion constraint expresses the rule more directly.
