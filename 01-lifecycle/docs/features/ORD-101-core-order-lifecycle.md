# ORD-101 - Core Order Lifecycle

## Problem

The system needs a reliable way to represent how an Order moves through its lifecycle without allowing callers to assign arbitrary states.

## Functional requirements

- A new Order starts in `CREATED`.
- `CREATED -> CONFIRMED` is allowed.
- `CONFIRMED -> SHIPPED` is allowed.
- `SHIPPED -> COMPLETED` is allowed.
- `CREATED -> CANCELLED` is allowed.
- `CONFIRMED -> CANCELLED` is allowed.
- All other direct transitions are rejected.

## Design choice

The Order entity owns its lifecycle transitions through behavior methods such as `confirm()`, `ship()`, `complete()`, and `cancel()`.

We intentionally do **not** introduce Spring Boot, a repository, a database, or messaging in this feature. None of those technologies are required to express or protect the lifecycle rule in a single-process model.

## Acceptance criteria traceability

| Requirement | Executable proof |
| --- | --- |
| New order starts CREATED | `OrderTest#newOrderStartsCreated` |
| Happy path reaches COMPLETED | `OrderTest#followsHappyPathToCompletion` |
| CREATED can cancel | `OrderTest#createdOrderCanBeCancelled` |
| CONFIRMED can cancel | `OrderTest#confirmedOrderCanBeCancelled` |
| CREATED cannot ship | `OrderTest#cannotShipCreatedOrder` |
| COMPLETED cannot cancel | `OrderTest#cannotCancelCompletedOrder` |

## Next pressure

The next feature will introduce application use cases and a repository boundary. That change is justified when callers should no longer construct and mutate Orders directly from the composition root.
