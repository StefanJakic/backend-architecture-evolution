# 05 - Behavior / Policy

## Status

**0.2.0-SNAPSHOT - Growing Rules Justify Policy Extraction**

This focused skeleton studies behavior variation and the point at which an abstraction becomes justified.

## Dominant question

> When the same use case behaves differently under different business rules, where should that variation live?

## Evolution so far

### POL-101 - Keep small variation local

The first version used:

~~~text
CancellationService
    |
    +-- switch(FLEXIBLE / STRICT)
~~~

With two short branches, that was intentionally the simpler design.

### POL-110 - Extract behavior after real divergence

The policy curves now differ independently:

~~~text
FLEXIBLE
  >= 72h -> 100%
  >= 24h -> 75%
  < 24h  -> 50%

STRICT
  >= 336h -> 100%
  >= 168h -> 50%
  < 168h  -> 0%
~~~

The design becomes:

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

## Service responsibility

`CancellationService` no longer knows:

- FLEXIBLE thresholds
- STRICT thresholds
- refund percentages
- policy-specific branching

It only delegates the variable behavior.

## Key lesson

~~~text
variation exists
!=
abstraction is automatically justified
~~~

but also:

~~~text
independently growing variation
=
real pressure for abstraction
~~~

POL-101 was not a mistake that POL-110 fixes.

The design changed because the problem changed.

## Why composition?

The use case remains cancellation.

Only the refund behavior varies.

Therefore:

~~~text
CancellationService HAS-A CancellationPolicy
~~~

is more accurate than creating a service inheritance hierarchy.

## Evolution

~~~text
POL-101  small policy variation stays local
POL-110  growing rules justify policy extraction      <- current
POL-120  select policy by business context
POL-130  composition vs inheritance / branch trade-offs
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

We now know how each policy behaves.

POL-120 asks a separate question:

> Given business context such as CancellationTerms, who chooses the correct policy?

That selection should not push FLEXIBLE/STRICT branching back into the use-case service.
