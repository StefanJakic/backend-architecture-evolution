# AGG-101 - Let Aggregate Root Own Child Mutation

## Goal

Model one invariant that spans a root and its child collection.

~~~text
Project
  totalBudget
  allocations[]
~~~

The business rule is:

~~~text
sum(allocation.amount)
<=
project.totalBudget
~~~

## Why this is an aggregate problem

A single allocation cannot know whether it is valid in isolation.

~~~text
Allocation(4_000)
~~~

may be valid for a project with 10_000 remaining and invalid for one with only 3_000 remaining.

The invariant lives across:

~~~text
Project.totalBudget
+
all Project.allocations
~~~

That makes the project the natural consistency owner.

## Aggregate root

~~~text
outside world
    |
    v
Project.addAllocation(...)
    |
    +-- validate child
    +-- reject duplicate ID
    +-- check remaining budget
    +-- mutate collection
~~~

Callers do not mutate the internal collection directly.

## Why immutable children help

`BudgetAllocation` is immutable.

A caller cannot obtain an allocation and later change its amount behind the root's back.

That keeps all state transitions capable of breaking the invariant inside the aggregate boundary.

## Failed mutations are atomic in memory

The root validates before changing the collection.

So:

~~~text
existing allocated = 7_000
remaining = 3_000

attempt add 4_000
    -> reject

state remains:
allocated = 7_000
remaining = 3_000
~~~

The aggregate never exposes an intermediate invalid state.

## Important distinction

An aggregate is not simply:

> an object graph loaded together

The important idea is:

> which state must be consistent together when one business operation completes?

For this skeleton:

~~~text
Project
+
its BudgetAllocations
=
one consistency boundary
~~~

## Why no repository yet?

Persistence does not define the aggregate.

The business invariant does.

POL-style architecture would be backwards if we started from tables/repositories and only later decided what must stay consistent.

AGG-110 will add persistence after the ownership boundary already exists.

## Next pressure

AGG-110 will persist the root and children while preserving one aggregate-level save/load boundary.
