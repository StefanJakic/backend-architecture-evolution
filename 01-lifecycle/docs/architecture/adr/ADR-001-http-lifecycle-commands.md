# ADR-001 - Represent lifecycle transitions as explicit HTTP commands

## Status

Accepted

## Context

The Order lifecycle supports a constrained set of business transitions. We need an HTTP representation for those transitions.

A generic `PATCH /orders/{id}` endpoint accepting a desired status would be resource-oriented, but it also suggests that status is freely assignable data.

That is not true in this domain. Status is the result of legal business actions.

## Decision

Expose explicit command endpoints:

- `POST /orders/{id}/confirm`
- `POST /orders/{id}/ship`
- `POST /orders/{id}/complete`
- `POST /orders/{id}/cancel`

Controllers delegate to application use cases and never set status directly.

## Consequences

### Positive

- API intent matches domain behavior.
- Illegal transitions still pass through the same domain invariant.
- Adding transition-specific input later remains straightforward.

### Negative

- The API is more command-oriented than a generic CRUD resource API.
- More endpoints exist than with one generic update endpoint.

## Alternatives considered

### PATCH with desired status

Rejected because it weakens the conceptual boundary around lifecycle transitions and makes arbitrary state assignment look valid.

### PUT the entire Order resource

Rejected because clients should not own server-managed lifecycle state.
