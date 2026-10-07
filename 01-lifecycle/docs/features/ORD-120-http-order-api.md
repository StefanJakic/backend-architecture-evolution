# ORD-120 - HTTP Order API

## Problem

ORD-110 introduced stable application use cases, but external clients still have no network-facing entry point.

The goal of this feature is to add HTTP delivery without moving lifecycle rules into controllers or coupling domain objects to Spring.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/orders` | Create an Order |
| GET | `/orders/{orderId}` | Read current status |
| POST | `/orders/{orderId}/confirm` | Confirm an Order |
| POST | `/orders/{orderId}/ship` | Ship an Order |
| POST | `/orders/{orderId}/complete` | Complete an Order |
| POST | `/orders/{orderId}/cancel` | Cancel an Order |

## Error mapping

- Unknown Order -> `404 Not Found`
- Invalid lifecycle transition -> `409 Conflict`

## Architecture

```text
HTTP
  -> OrderController
      -> OrderService
          -> OrderRepository
              -> InMemoryOrderRepository
          -> Order
```

Spring Boot now owns runtime startup and dependency composition, but the domain and application layers remain plain Java.

## Why command endpoints instead of PATCH status?

The API exposes business actions rather than arbitrary state assignment.

A generic request such as:

```http
PATCH /orders/{id}
{ "status": "SHIPPED" }
```

would make the transport layer appear able to assign lifecycle state directly.

Explicit commands such as:

```http
POST /orders/{id}/ship
```

communicate intent and still force the request through `Order.ship()`, where the invariant is enforced.

## Acceptance criteria traceability

| Requirement | Executable proof |
| --- | --- |
| Create and read Order over HTTP | `OrderHttpApiTest#createsAndReadsOrder` |
| Execute lifecycle through HTTP | `OrderHttpApiTest#executesLifecycleThroughHttp` |
| Unknown Order returns 404 | `OrderHttpApiTest#returnsNotFoundForUnknownOrder` |
| Invalid transition returns 409 | `OrderHttpApiTest#returnsConflictForInvalidTransition` |

## Deferred concerns

This version still uses in-memory storage. Restarting the application loses all Orders.

That limitation is intentional and creates the requirement for the next feature: durable persistence.
