# 06 - Aggregate / Ownership

## Status

**0.3.0-SNAPSHOT - Root-Level Optimistic Versioning**

This focused skeleton studies consistency and ownership boundaries.

## Dominant question

> Which objects must change together to protect one business invariant, and where should the consistency boundary live?

## Domain

~~~text
Project
  totalBudget
  version
  allocations[]
~~~

Invariant:

~~~text
sum(allocation.amount)
<=
project.totalBudget
~~~

## AGG-101 - Root owns child mutation

The root is the only entry point for allocation changes.

~~~text
outside world
    |
    v
Project.addAllocation(...)
~~~

## AGG-110 - Persistence follows the aggregate

~~~text
ProjectRepository
    |
    +-- insert(Project)
    +-- findById(projectId)
    +-- save(Project)
~~~

There is no independent BudgetAllocationRepository.

Multiple relational tables still represent one domain consistency boundary.

## AGG-120 - Concurrent snapshots need root-level versioning

Before versioning:

~~~text
Tx A loads Project v0
Tx B loads Project v0

A adds Allocation-A
B adds Allocation-B

A saves
B saves later
~~~

Both calls could succeed while one child change disappeared.

That is a whole-aggregate lost update.

## Optimistic root save

Save now begins with:

~~~sql
UPDATE project
SET version = version + 1
WHERE id = ?
  AND version = ?
~~~

With two transactions loaded from version 0:

~~~text
exactly 1 SUCCESS
exactly 1 CONFLICT
~~~

Final persisted state:

~~~text
initial allocation
+
winner allocation
+
version = 1
~~~

The stale transaction fails before replacing child rows.

## Why version the root?

The competing transactions may add different children.

They still conflict because both computed new aggregate state from the same prior snapshot.

Therefore:

~~~text
aggregate conflict
-> root version
~~~

not:

~~~text
one version per child
~~~

## Current lesson

~~~text
domain invariant
-> protects mutation inside one loaded aggregate

optimistic root version
-> protects against stale concurrent aggregate saves
~~~

## Evolution

~~~text
AGG-101  root owns child mutation + invariant
AGG-110  persist aggregate as one consistency boundary
AGG-120  concurrent updates + optimistic version      <- current
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next

After verification, 06-aggregate is ready for **1.0 COMPLETE / FROZEN**.
