# ADR-002 - Keep JPA mapping outside the Order domain model

## Status

Accepted

## Context

ORD-130 introduces PostgreSQL and JPA.

The simplest implementation would annotate the existing `Order` class with `@Entity`, `@Id`, and persistence-specific mapping annotations.

That would reduce the number of classes, but it would also make the domain model depend directly on the ORM.

## Decision

Keep `Order` as a plain Java domain object and introduce `OrderJpaEntity` in the infrastructure layer.

`PostgresOrderRepository` maps between the two models.

## Consequences

### Positive

- Domain behavior remains readable without ORM concerns.
- Persistence technology can evolve behind the existing repository port.
- JPA constructor and mapping requirements do not shape the domain API.
- Domain tests remain framework-free.

### Negative

- A small amount of mapping code exists.
- Domain and persistence representations must evolve together when persisted fields change.

## Alternatives considered

### Annotate Order directly with JPA

Rejected for this project because the repository is explicitly teaching architectural boundaries, and persistence annotations would blur a boundary that was established before JPA existed.

For a simple CRUD application, using the same model for domain and persistence can still be a reasonable trade-off.
