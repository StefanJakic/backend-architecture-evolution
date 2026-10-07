# ORD-110 - Application Use Cases

## Problem

The domain model from ORD-101 protects lifecycle transitions, but callers still need a stable application boundary for executing use cases by Order identifier.

Without that boundary, every caller would need to know how to locate an Order, invoke domain behavior, and persist the result.

## Functional requirements

- Create an Order and return its identifier.
- Confirm, ship, complete, and cancel an Order by identifier.
- Reject commands for unknown Orders.
- Keep lifecycle transition rules inside the Order domain entity.

## Design

The application flow is now:

```text
caller
  -> OrderService
      -> OrderRepository.findById(...)
      -> Order.confirm()/ship()/complete()/cancel()
      -> OrderRepository.save(...)
```

`OrderRepository` is a port owned by the application boundary. The current adapter is `InMemoryOrderRepository`.

The in-memory implementation is deliberately temporary. Its purpose is to prove that application code depends on a storage abstraction before a database technology is selected.

## Why no Spring Boot yet?

Constructor wiring in `LifecycleDemo` already demonstrates dependency injection:

```text
InMemoryOrderRepository
        ↓
   OrderService
        ↓
      Main
```

Spring will later become the composition mechanism and HTTP runtime. It is not required to define the service or repository boundary.

## Acceptance criteria traceability

| Requirement | Executable proof |
| --- | --- |
| Create and store an Order | `OrderServiceTest#createsAndStoresOrder` |
| Execute lifecycle use cases by id | `OrderServiceTest#executesLifecycleUseCasesById` |
| Domain still owns transition rules | `OrderServiceTest#cancellationStillUsesDomainRules` |
| Reject unknown Orders | `OrderServiceTest#unknownOrderIsRejectedAtApplicationBoundary` |

## Architectural lesson

The repository interface exists because application code needs a persistence boundary, not because every domain object should automatically receive a repository.

The next feature will introduce Spring Boot and REST as an inbound delivery mechanism while preserving the same application and domain boundaries.
