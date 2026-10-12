# 06 - Aggregate / Ownership

## Status

**0.2.0-SNAPSHOT - Aggregate Persistence Boundary**

This focused skeleton studies consistency and ownership boundaries.

## Dominant question

> Which objects must change together to protect one business invariant, and where should the consistency boundary live?

## Domain

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

## AGG-101 - Root owns child mutation

~~~text
outside world
    |
    v
Project.addAllocation(...)
    |
    +-- validate duplicate ID
    +-- check remaining budget
    +-- mutate child collection
~~~

Children are immutable and the internal collection is not externally mutable.

## AGG-110 - Persistence follows the aggregate

Persistence now exposes one repository:

~~~text
ProjectRepository
    |
    +-- insert(Project)
    +-- findById(projectId)
    +-- save(Project)
~~~

There is deliberately no:

~~~text
BudgetAllocationRepository
~~~

The application transaction is:

~~~text
load Project
    ↓
mutate through root
    ↓
save Project
    ↓
commit
~~~

## Relational tables are not aggregate boundaries

Storage uses:

~~~text
project
budget_allocation
~~~

but application code still works through:

~~~text
Project aggregate
~~~

Multiple tables do not imply multiple independently mutable domain objects.

## Rehydration

The repository loads root and children in one JOIN snapshot and reconstructs through:

~~~text
Project.restore(...)
~~~

which reuses root validation.

Persistence does not bypass the domain invariant.

## Current lesson

~~~text
domain consistency boundary
should drive
persistence entry points
~~~

not the other way around.

## Evolution

~~~text
AGG-101  root owns child mutation + invariant
AGG-110  persist aggregate as one consistency boundary  <- current
AGG-120  concurrent aggregate updates / optimistic version
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

AGG-120 will run two concurrent load-modify-save operations against the same Project.

Both can start from the same valid aggregate snapshot and each make a locally valid change.

Without root-level versioning, the later whole-aggregate save can overwrite the earlier one.
