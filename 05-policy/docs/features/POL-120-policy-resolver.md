# POL-120 - Resolve Policy from Business Context

## Pressure

POL-110 separated policy behavior into:

~~~text
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

But the caller still had to know which concrete class to construct.

That leaks implementation knowledge outside the use case.

## New design

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

The business label and behavior implementation are connected in one explicit place.

## Responsibilities

### CancellationTerms

Answers:

> Which business terms apply?

Example:

~~~text
FLEXIBLE
STRICT
~~~

### CancellationPolicyResolver

Answers:

> Which behavior implements those terms?

~~~text
FLEXIBLE -> FlexibleCancellationPolicy
STRICT   -> StrictCancellationPolicy
~~~

### CancellationPolicy

Answers:

> How is the refund calculated?

### CancellationService

Answers:

> When should the use case execute the selected behavior?

The service does not know the thresholds or concrete policy classes.

## Why EnumMap?

There are only a few known enum values.

An explicit map is:

- readable
- deterministic
- easy to test
- easy to fail on missing registration
- free of reflection/framework magic

The design does not need classpath scanning or annotations just to avoid a small explicit mapping.

## Where is the switch now?

There is no FLEXIBLE/STRICT switch in the service.

The selection concern is centralized as data:

~~~text
CancellationTerms -> CancellationPolicy
~~~

That is different from embedding policy behavior in branches.

## Missing registrations

Resolver construction may intentionally omit a policy.

If a caller asks for unregistered terms, resolution fails clearly.

This is preferable to silently falling back to unrelated behavior.

## Standard wiring

For this framework-free skeleton:

~~~text
CancellationPolicyResolver.standard()
~~~

creates the default mapping.

A future DI framework could assemble the same relationship differently, but the architectural boundary would remain the same.

## Important lesson

Behavior extraction and behavior selection are separate pressures:

~~~text
POL-110
How should each policy behave?

POL-120
Who decides which policy applies?
~~~

Separating them prevents the application service from becoming both an orchestrator and a policy registry.

## Next pressure

POL-130 will compare:

- a local branch
- composition through policy objects
- inheritance

and record when each is the clearest representation rather than treating any one technique as universally correct.
