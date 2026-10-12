# AGG-110 - Persist Aggregate as One Consistency Boundary

## Pressure

AGG-101 established:

~~~text
Project
+
BudgetAllocation children
=
one consistency boundary
~~~

The next risk is allowing persistence to break that boundary.

A tempting design would be:

~~~text
ProjectRepository
BudgetAllocationRepository
~~~

That would let application code update a child independently from the root.

The domain invariant could then be bypassed.

## Persistence boundary

This module exposes only:

~~~text
ProjectRepository
~~~

with aggregate-level operations:

~~~text
insert(Project)
findById(projectId)
save(Project)
~~~

There is no child repository.

## Application transaction

The use case is:

~~~text
BEGIN

load Project aggregate
    ↓
project.addAllocation(...)
    ↓
save Project aggregate

COMMIT
~~~

The application service owns that transaction.

The domain root still owns the business mutation.

## Load as one snapshot

Project and child rows are loaded with one SQL statement:

~~~sql
SELECT
    p.id,
    p.total_budget_cents,
    a.allocation_id,
    a.amount_cents
FROM project p
LEFT JOIN budget_allocation a
  ON a.project_id = p.id
WHERE p.id = ?
ORDER BY a.allocation_id
~~~

That avoids reconstructing one aggregate from separate root and child SELECT statements taken at different statement snapshots.

## Rehydration still protects the invariant

Persistence does not receive a special back door into Project state.

Rows are reconstructed through:

~~~text
Project.restore(...)
    ↓
Project.addAllocation(...)
~~~

So invalid persisted child state cannot silently create an invalid domain object.

The domain remains the authority for the invariant.

## Save strategy

For this learning step, save uses aggregate snapshot replacement:

~~~text
update root
delete persisted child rows
insert current child snapshot
~~~

This is intentionally simple.

Because the save occurs inside a transaction, callers never observe a partially replaced aggregate after commit.

AGG-120 will show the missing piece:

~~~text
two transactions
load same aggregate version
mutate independently
save independently
~~~

Without concurrency control, one whole-aggregate snapshot can overwrite another.

## Failed domain mutation

If:

~~~text
budget = 10_000
allocated = 8_000
attempt = 3_000
~~~

then:

~~~text
Project.addAllocation(...)
-> throws before repository.save(...)
~~~

and persisted state remains:

~~~text
allocated = 8_000
remaining = 2_000
~~~

## Important lesson

A relational model may use multiple tables:

~~~text
project
budget_allocation
~~~

while the domain still exposes one consistency boundary:

~~~text
Project aggregate
~~~

Table boundaries and aggregate boundaries are not the same thing.

## Next pressure

AGG-120 will reproduce concurrent load-modify-save and add optimistic versioning to the aggregate root.
