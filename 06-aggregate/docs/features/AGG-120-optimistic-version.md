# AGG-120 - Protect Concurrent Aggregate Updates with Optimistic Versioning

## Pressure

AGG-110 persists one Project aggregate as a whole snapshot.

Initial state:

~~~text
Project budget = 10_000
Allocation-0 = 2_000
version = 0
~~~

Two transactions can both load the same valid snapshot:

~~~text
Tx A                          Tx B

load Project v0               load Project v0
+ Allocation-A 3_000          + Allocation-B 4_000
~~~

Both domain mutations are locally valid.

## Lost update before the fix

Without versioning, both services could report success while the later whole-aggregate save replaced the earlier child snapshot.

Broken outcome:

~~~text
both service calls = SUCCESS

final aggregate:
Allocation-0
+ only one of {Allocation-A, Allocation-B}
~~~

The aggregate invariant can still look valid while one successful business change disappears.

## Root-level conflict

The transactions do not need to modify the same child row.

They conflict because both derive a new whole-aggregate state from the same old Project snapshot.

So the concurrency token belongs to the aggregate root.

## Optimistic save

Project now carries a version.

Save starts with:

~~~sql
UPDATE project
SET total_budget_cents = ?,
    version = version + 1
WHERE id = ?
  AND version = ?
~~~

If exactly one row updates, this transaction owns the next aggregate version.

If zero rows update, another transaction already advanced the root and the repository throws:

~~~text
OptimisticAggregateConflictException
~~~

before deleting or inserting any child rows.

## Concurrent result

Both transactions may still load version 0 and perform business work without holding a lock for the whole operation.

At save time:

~~~text
A: UPDATE ... WHERE version = 0
   -> success
   -> version becomes 1

B: UPDATE ... WHERE version = 0
   -> 0 rows
   -> conflict
~~~

or the winner may be reversed.

Required result:

~~~text
1 SUCCESS
1 CONFLICT
~~~

Final aggregate:

~~~text
Allocation-0
+ winner allocation
version = 1
~~~

The stale transaction persists no child state.

## What the version protects

The version column lives on the root table, but logically protects:

~~~text
Project fields
+
all BudgetAllocation children
=
one aggregate snapshot
~~~

This works because all writes go through the root repository transaction.

## Retry policy

This skeleton surfaces the conflict.

It does not auto-retry.

A real application could reload, reapply the command, and retry with limits, but that is application policy because a retry can change business meaning.

## Final lesson

~~~text
domain invariant
-> protects one in-memory aggregate mutation

root optimistic version
-> protects one persisted aggregate snapshot
~~~
