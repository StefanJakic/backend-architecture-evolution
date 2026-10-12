# 06 - Aggregate / Ownership

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton studies one dominant architecture question:

> Which objects must change together to protect one business invariant, and where should the consistency boundary live?

## Final learning path

~~~text
AGG-101  root owns child mutation + invariant
    ↓
AGG-110  persistence follows the aggregate boundary
    ↓
AGG-120  concurrent stale snapshots + optimistic root version
    ↓
1.0      Freeze
~~~

## 1. Aggregate starts from an invariant

Domain:

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

A single BudgetAllocation cannot decide whether it is valid in isolation.

The rule depends on the root and all existing children.

Therefore Project is the consistency owner.

## 2. Root owns child mutation

External code asks:

~~~text
project.addAllocation(...)
~~~

rather than mutating children directly.

The aggregate root:

- rejects duplicate allocation IDs
- checks remaining budget
- mutates the child collection only after validation
- exposes children as a read-only snapshot

BudgetAllocation is immutable.

This prevents callers from changing state behind the root's back.

## 3. Object graph is not enough

~~~text
object containment
!=
aggregate boundary
~~~

The boundary exists because state must be valid together after one business operation.

For this module:

~~~text
Project
+
all BudgetAllocations
=
one consistency boundary
~~~

## 4. Persistence follows the aggregate

Storage uses two relational tables:

~~~text
project
budget_allocation
~~~

but the application exposes only:

~~~text
ProjectRepository
    |
    +-- insert(Project)
    +-- findById(projectId)
    +-- save(Project)
~~~

There is deliberately no BudgetAllocationRepository.

Multiple tables do not imply multiple independently mutable domain roots.

## 5. Load, mutate, save is one application transaction

~~~text
BEGIN

load Project aggregate
    ↓
project.addAllocation(...)
    ↓
save Project aggregate

COMMIT
~~~

The service transaction coordinates the use case.

The domain root still owns the business invariant.

## 6. Rehydration does not bypass the domain

The repository reconstructs persisted child state through:

~~~text
Project.restore(...)
    ↓
Project.addAllocation(...)
~~~

Persistence does not get a special back door into invalid domain state.

## 7. Concurrent snapshots create a different problem

Even with a correct in-memory invariant, two transactions can load the same valid aggregate version:

~~~text
Tx A loads Project v0
Tx B loads Project v0

A adds Allocation-A
B adds Allocation-B
~~~

Both mutations may be locally valid.

Without concurrency control:

~~~text
A saves
B saves later
~~~

and the later whole-aggregate snapshot can overwrite the earlier one.

The final aggregate may still satisfy the budget invariant while one successful business change has disappeared.

That is a lost update at the aggregate level.

## 8. Root-level optimistic versioning

Project carries:

~~~text
version
~~~

Save begins with:

~~~sql
UPDATE project
SET total_budget_cents = ?,
    version = version + 1
WHERE id = ?
  AND version = ?
~~~

If:

~~~text
updated rows = 1
~~~

the transaction owns the next aggregate version and may replace child state.

If:

~~~text
updated rows = 0
~~~

the snapshot is stale and the repository throws an optimistic conflict before touching child rows.

## 9. Why version the root?

Concurrent transactions may add different children.

They still conflict because both computed a new aggregate state from the same prior root snapshot.

Therefore:

~~~text
aggregate conflict
-> root version
~~~

not:

~~~text
one version per child
~~~

The root version logically protects:

~~~text
Project fields
+
all BudgetAllocation children
=
one persisted aggregate snapshot
~~~

## 10. Two different kinds of protection

These solve different problems:

~~~text
domain invariant
-> protects one in-memory mutation

optimistic root version
-> protects against stale concurrent aggregate saves
~~~

Both matter.

A correct aggregate model without concurrency protection can still lose updates.

A version column without a proper domain owner can still persist invalid business state.

## Final architecture

~~~text
ProjectBudgetService
        |
        | transaction
        v
ProjectRepository
        |
        | load/save aggregate
        v
Project
  |
  +-- invariant owner
  +-- version
  +-- BudgetAllocation children
        |
        v
PostgreSQL
  project
  budget_allocation
~~~

## What this skeleton teaches

- aggregate root as mutation entry point
- invariants spanning root + children
- immutable child state
- consistency boundary vs object containment
- repository boundary aligned with aggregate boundary
- relational tables vs domain ownership
- aggregate rehydration
- load-modify-save transaction shape
- whole-aggregate lost updates
- optimistic concurrency control
- root-level version semantics
- stale snapshot conflict detection

## Deliberately not included

This module does not add:

- child repositories
- pessimistic locking
- automatic retries
- Kafka/events
- sagas
- distributed transactions
- leases
- generic repository abstractions

Those are different architecture pressures.

## Run

~~~bash
./gradlew test
~~~

## Freeze rule

This module is intentionally closed at **1.0.0**.

A new dominant pressure belongs in a new skeleton.

## Next

**07 - Workflow / Orchestration**

Dominant question:

> When one business operation spans multiple steps and dependencies, who owns the workflow state, failure handling, and compensation?
