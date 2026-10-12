# POL-130 - Branch vs Composition vs Inheritance

## Goal

Finish the behavior/policy learning path by choosing the simplest design shape for each kind of variation.

The important question is not:

> Which pattern is best?

It is:

> Which representation makes the current business variation easiest to understand and evolve?

## 1. Local branch

Shape:

~~~text
service
  |
  +-- if / switch
~~~

Use it when variation is:

- small
- local to one use case
- cohesive
- unlikely to evolve independently
- easier to read inline than across several types

This was POL-101.

~~~text
FLEXIBLE
STRICT
~~~

each had one tiny rule.

At that point, introducing multiple policy classes would have increased structure without reducing complexity.

### Good signal

~~~text
open one method
-> understand the whole variation
~~~

### Warning signal

The branch starts accumulating:

- different thresholds
- different calculations
- different helpers
- different external data
- different tests
- different reasons to change

At that point the behavior is no longer truly local.

## 2. Composition

Shape:

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

Use it when:

- one part of a use case varies
- variants share a meaningful contract
- variants evolve independently
- each behavior deserves focused tests
- runtime/business context may select the behavior

This became the better design in POL-110.

The service still represents one use case:

~~~text
cancel reservation
~~~

Only the refund behavior varies.

That means:

~~~text
CancellationService HAS-A CancellationPolicy
~~~

is a more accurate model than creating multiple cancellation-service subtypes.

## 3. Inheritance

Possible shape:

~~~text
CancellationService
       ^
       |
   ------------
   |          |
Flexible   Strict
Service    Service
~~~

This is deliberately **not** implemented.

Inheritance is useful when there is a real stable subtype relationship.

Good signals include:

- the subtype truly **is a** specialized form of the base concept
- callers can safely substitute any subtype for the base type
- the base type owns meaningful shared lifecycle or invariants
- subclasses refine a coherent extension point
- the hierarchy is stable and understandable

Those conditions are not present here.

Flexible cancellation is not a different kind of cancellation use case.

It is the same use case with different refund behavior.

Subclassing the service would couple behavior variation to the whole service type.

## Why service inheritance would be weaker here

Imagine:

~~~text
FlexibleCancellationService
StrictCancellationService
~~~

Now policy choice also means service choice.

If cancellation later gains shared orchestration:

~~~text
validate cancellation
calculate refund
record cancellation
emit event
~~~

the subclasses inherit or override a larger lifecycle even though only one step varies.

That increases the risk of:

- duplicated orchestration
- overridden lifecycle steps
- fragile base-class coupling
- policy behavior leaking into service hierarchy

Composition keeps the stable workflow in one service and varies only the part that actually changes.

## 4. Selection is a separate concern

POL-120 added:

~~~text
CancellationTerms
        |
        v
CancellationPolicyResolver
        |
        v
CancellationPolicy
~~~

This does not make the resolver a new policy layer.

It answers a different question:

~~~text
policy
-> how behavior works

resolver
-> which policy applies
~~~

Keeping those separate prevents the application service from becoming both orchestrator and registry.

## Decision matrix

| Pressure | Prefer | Why |
| --- | --- | --- |
| Two or three tiny local cases | Local branch | Lowest cognitive overhead |
| Behavior grows independently | Composition | Behavior owns its own rules |
| Behavior chosen by runtime/business context | Composition + explicit resolver | Selection stays separate from execution |
| Stable true subtype with shared lifecycle/invariants | Inheritance may fit | Real is-a relationship |
| Only one step of a larger use case varies | Composition | Vary the part, not the whole service |

## Important nuance

Composition is not automatically better than every branch.

POL-101 and POL-110 are both correct designs for different levels of pressure.

Likewise, inheritance is not forbidden.

It simply needs stronger justification than:

> these two classes do something slightly differently.

## What about configuration-driven rules?

If policy variation became mostly data:

~~~text
threshold -> percentage
threshold -> percentage
~~~

a table/configuration model might eventually be simpler than one class per policy.

That would be a different pressure.

This skeleton intentionally stops before turning into a generic rules engine.

## Final decision for this module

~~~text
small local variation
    -> branch

independently evolving behavior
    -> composition

business-context selection
    -> explicit resolver

service inheritance
    -> not justified
~~~

## Final lesson

Abstraction should follow pressure.

Do not replace a readable branch merely because a named pattern exists.

Do not keep a growing branch merely because fewer classes look simpler.

Choose the shape that keeps responsibilities and reasons-to-change clear.
