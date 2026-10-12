# 06 - Aggregate / Ownership

## Status

**0.1.0-SNAPSHOT - Aggregate Root Owns Child Mutation**

This focused skeleton studies consistency and ownership boundaries.

## Dominant question

> Which objects must change together to protect one business invariant, and where should the consistency boundary live?

## Current domain

~~~text
Project
  totalBudget
  allocations[]
~~~

Invariant:

~~~text
sum(allocation.amount)
<=
project.totalBudget
~~~

## Current design

~~~text
outside world
    |
    v
Project
    |
    +-- addAllocation(...)
    +-- remainingBudgetCents()
    +-- allocations() -> read-only snapshot
    |
    +-- BudgetAllocation
    +-- BudgetAllocation
    +-- BudgetAllocation
~~~

`Project` is the aggregate root.

## Why the root owns mutation

A `BudgetAllocation` cannot decide whether its amount fits the total project budget.

That decision requires state from the whole aggregate.

Therefore callers ask the root to mutate:

~~~text
project.addAllocation(...)
~~~

rather than editing children directly.

## Key lesson

~~~text
object containment
!=
aggregate boundary
~~~

The boundary exists because several pieces of state must remain consistent together.

## Planned evolution

~~~text
AGG-101  root owns child mutation + invariant       <- current
AGG-110  persist aggregate as one consistency boundary
AGG-120  concurrent aggregate updates / optimistic version
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

AGG-110 will add persistence without giving children independent repository ownership.
