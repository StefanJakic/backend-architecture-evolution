# 05 - Behavior / Policy

## Status

**0.3.0-SNAPSHOT - Policy Resolution by Business Context**

This focused skeleton studies behavior variation and the point at which an abstraction becomes justified.

## Dominant question

> When the same use case behaves differently under different business rules, where should that variation live?

## Evolution so far

### POL-101 - Keep small variation local

~~~text
CancellationService
    |
    +-- switch(FLEXIBLE / STRICT)
~~~

With two short branches, the local switch was the clearest design.

### POL-110 - Extract independently growing behavior

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

The service stopped owning policy-specific thresholds and percentages.

### POL-120 - Separate policy selection from policy behavior

Callers now provide a business label:

~~~text
CancellationTerms
~~~

which is resolved through:

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

## Responsibility split

~~~text
CancellationTerms
    -> which business terms apply

CancellationPolicyResolver
    -> which behavior implements those terms

CancellationPolicy
    -> how refund behavior works

CancellationService
    -> execute the use case
~~~

The service contains no FLEXIBLE/STRICT branch.

## Why explicit mapping?

The standard resolver uses an explicit enum mapping.

For two known policies this is clearer than:

- reflection
- annotations
- service discovery
- framework scanning

Abstraction should remove complexity, not hide a tiny amount of wiring behind magic.

## Current lesson

~~~text
behavior variation
and
behavior selection
are different responsibilities
~~~

POL-110 solved the first.

POL-120 solves the second.

## Evolution

~~~text
POL-101  small policy variation stays local
POL-110  growing rules justify policy extraction
POL-120  select policy by business context           <- current
POL-130  composition vs inheritance / branch trade-offs
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

POL-130 will finish the learning path by comparing the three shapes directly:

~~~text
local branch
composition
inheritance
~~~

and documenting the selection criteria instead of declaring one pattern universally best.
