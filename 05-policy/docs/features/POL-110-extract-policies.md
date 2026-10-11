# POL-110 - Extract Policies When Rules Diverge

## Pressure

POL-101 intentionally kept the variation local:

~~~text
CancellationService
    |
    +-- switch(FLEXIBLE / STRICT)
~~~

That was the simpler design while both branches contained one small rule.

The policies now have independent refund curves.

Flexible:

~~~text
>= 72h -> 100%
>= 24h -> 75%
< 24h  -> 50%
~~~

Strict:

~~~text
>= 336h -> 100%
>= 168h -> 50%
< 168h  -> 0%
~~~

Keeping these rules inside one service would make the service responsible for:

- use-case orchestration
- flexible thresholds
- flexible percentages
- strict thresholds
- strict percentages
- all future policy-specific helpers

That is the pressure that justifies extraction.

## New design

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

The application service now performs:

~~~text
request
    ↓
selected policy
    ↓
policy.calculate(request)
    ↓
RefundDecision
~~~

It contains no FLEXIBLE/STRICT branch.

## Why this is better now

Each policy can evolve independently.

Tests also move next to the behavior:

~~~text
FlexibleCancellationPolicyTest
StrictCancellationPolicyTest
~~~

The service test only proves delegation.

This separates two questions:

~~~text
What does FLEXIBLE mean?
    -> FlexibleCancellationPolicy

How is cancellation executed?
    -> CancellationService
~~~

## Composition, not service inheritance

We did not create:

~~~text
FlexibleCancellationService
StrictCancellationService
~~~

The use case itself is still the same.

Only one part of its behavior varies.

So the service **uses** a policy instead of being subclassed by policy type.

## Why keep CancellationTerms?

The enum remains the business label for the available terms.

POL-110 does not use it for branching anymore.

POL-120 will use business context to resolve:

~~~text
CancellationTerms
    ↓
CancellationPolicy
~~~

That selection concern is intentionally separate from the behavior itself.

## Important lesson

POL-101 and POL-110 are both valid designs at different stages.

~~~text
small local variation
    -> switch can be clearer

independently growing behavior
    -> policy objects earn their place
~~~

The pattern is a response to pressure, not the starting goal.
