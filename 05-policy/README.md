# 05 - Behavior / Policy

## Status

**0.4.0-SNAPSHOT - Branch, Composition, and Inheritance Trade-offs**

This focused skeleton studies behavior variation and the point at which an abstraction becomes justified.

## Dominant question

> When the same use case behaves differently under different business rules, where should that variation live?

## Learning path

~~~text
POL-101  small variation stays local
    ↓
POL-110  independent behavior earns policy objects
    ↓
POL-120  business context resolves the policy
    ↓
POL-130  choose branch / composition / inheritance deliberately
~~~

## 1. Start with the smallest readable shape

POL-101:

~~~text
CancellationService
    |
    +-- switch(FLEXIBLE / STRICT)
~~~

With two tiny rules, the branch was easier to understand than an interface plus several classes.

Lesson:

~~~text
variation exists
!=
abstraction is automatically justified
~~~

## 2. Extract behavior when it develops its own complexity

POL-110:

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

Each policy acquired its own thresholds and refund curve.

The abstraction became useful because the behaviors had different reasons to change.

## 3. Keep behavior selection separate

POL-120:

~~~text
CancellationTerms
        |
        v
CancellationPolicyResolver
        |
        v
CancellationPolicy
        |
        v
CancellationService
~~~

Responsibilities:

~~~text
Terms
    -> which business terms apply

Resolver
    -> which implementation matches those terms

Policy
    -> how refund behavior works

Service
    -> execute the use case
~~~

## 4. Branch vs composition vs inheritance

### Local branch

Prefer when variation is:

- small
- cohesive
- local
- easier to scan inline

### Composition

Prefer when:

- only part of the use case varies
- variants share a stable contract
- behavior evolves independently
- behavior may be selected dynamically

### Inheritance

Consider only when:

- there is a real stable is-a relationship
- subclasses are safely substitutable
- the base type owns meaningful shared lifecycle/invariants

Do not subclass a service merely because one calculation varies.

For this use case:

~~~text
CancellationService HAS-A CancellationPolicy
~~~

is more precise than:

~~~text
FlexibleCancellationService IS-A CancellationService
StrictCancellationService IS-A CancellationService
~~~

## Decision matrix

~~~text
tiny local variation
    -> branch

independently growing behavior
    -> composition

runtime/business selection
    -> composition + resolver

true stable subtype hierarchy
    -> inheritance may fit
~~~

## Important lesson

POL-101 was not a bad design that POL-110 repaired.

The problem acquired more pressure.

The design evolved with it.

~~~text
simple problem
-> simple representation

growing independent behavior
-> explicit abstraction
~~~

## Evolution

~~~text
POL-101  small policy variation stays local
POL-110  growing rules justify policy extraction
POL-120  select policy by business context
POL-130  composition vs inheritance / branch trade-offs  <- current
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next

No new runtime feature is needed.

After verification, this module is ready for **1.0 COMPLETE / FROZEN**.
