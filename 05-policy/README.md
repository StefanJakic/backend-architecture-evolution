# 05 - Behavior / Policy

## Status

**0.1.0-SNAPSHOT - Small Policy Variation Stays Local**

This focused skeleton studies behavior variation and the point at which an abstraction becomes justified.

## Dominant question

> When the same use case behaves differently under different business rules, where should that variation live?

## Current use case

Cancellation refund calculation.

~~~text
FLEXIBLE
  >= 24h -> 100%
  < 24h  -> 50%

STRICT
  >= 168h -> 100%
  < 168h  -> 0%
~~~

## Current design

~~~text
CancellationService
    |
    +-- explicit switch
~~~

There is intentionally **no policy interface yet**.

With two short branches, the switch is currently easier to read than several tiny strategy classes.

## Current lesson

~~~text
variation exists
!=
abstraction is automatically justified
~~~

A local branch is acceptable while the behavior is small and cohesive.

## Planned evolution

~~~text
POL-101  small policy variation stays local
POL-110  growing rules justify policy extraction
POL-120  select policy by business context
POL-130  composition vs inheritance / branch trade-offs
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

POL-110 will make the policies diverge enough that the service starts owning too much policy-specific behavior.

That is the point where composition through explicit policy objects should become simpler than the switch.
