# RSV-140 - Reservation Skeleton 1.0 Freeze

## Status

Complete.

## Scope

No new business behavior is introduced in this release.

The purpose of 1.0 is to mark the learning objective as complete and prevent the module from expanding into unrelated infrastructure work.

## Completed pressures

~~~text
time semantics
    ↓
concurrent check-then-insert race
    ↓
database-enforced no-overlap
    ↓
temporary hold
    ↓
expiry transaction boundary
~~~

## Why freeze now?

The skeleton can already answer the questions it was created to teach:

- what exactly counts as an overlapping time range?
- why can two correct application checks still double-book?
- when does a database constraint provide a stronger guarantee than application code?
- why is optimistic locking not the natural answer for two different INSERTs?
- how should temporary capacity expire?
- what happens when cleanup and the triggering command share the wrong transaction boundary?

Adding HTTP, Kafka, Redis, deployment, or payment integration would mostly repeat concerns already covered elsewhere.

## Build reproducibility

The module now commits a Gradle 8.14.3 wrapper.

That choice also captures a real engineering lesson discovered during verification: a machine-global Gradle version can make a correct project fail before tests even start.

The repository therefore owns the build-tool version.

## Next pressure

The next skeleton changes dimensions from time to quantity:

~~~text
03-inventory

available = 1

Request A wants 1
Request B wants 1

How do we guarantee inventory never becomes invalid?
~~~

That will introduce overselling, atomic decrement, contention, and inventory reservation semantics without repeating the full lifecycle stack.
