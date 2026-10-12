# 05 - Behavior / Policy

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton studies one dominant design question:

> When the same use case behaves differently under different business rules, where should that variation live?

## Final learning path

~~~text
POL-101  small policy variation stays local
    ↓
POL-110  growing rules justify policy extraction
    ↓
POL-120  resolve policy from business context
    ↓
POL-130  branch vs composition vs inheritance trade-offs
    ↓
1.0      Freeze
~~~

## 1. Small variation can stay local

The first version used one explicit branch:

~~~text
CancellationService
    |
    +-- switch(FLEXIBLE / STRICT)
~~~

With only two tiny rules, the switch was easier to understand than an interface plus multiple classes.

The lesson was:

~~~text
variation exists
!=
abstraction is automatically justified
~~~

A readable branch is not a design failure.

## 2. Independent behavior earns its own abstraction

The cancellation rules later developed separate thresholds and refund curves.

At that point the service would have started owning:

- policy-specific thresholds
- policy-specific percentages
- policy-specific helpers
- policy-specific tests
- several independent reasons to change

The design evolved to:

~~~text
CancellationService
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
~~~

The abstraction appeared because the behavior developed independent complexity.

## 3. Selection and behavior are different concerns

Once concrete policies existed, another question appeared:

> Who decides which policy applies?

That produced:

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

Responsibility split:

~~~text
CancellationTerms
    -> which business terms apply

CancellationPolicyResolver
    -> which implementation matches those terms

CancellationPolicy
    -> how refund behavior works

CancellationService
    -> execute the use case
~~~

The application service contains no FLEXIBLE/STRICT branching.

## 4. Why the resolver stays explicit

The standard resolver uses a simple enum mapping:

~~~text
FLEXIBLE -> FlexibleCancellationPolicy
STRICT   -> StrictCancellationPolicy
~~~

That is intentionally preferred over:

- reflection
- classpath scanning
- annotations
- service discovery
- framework magic

For a few known policy types, explicit wiring is easier to reason about.

## 5. Branch vs composition vs inheritance

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
- behavior may be selected from runtime/business context

### Inheritance

Consider only when:

- there is a real stable is-a relationship
- subclasses are safely substitutable
- the base type owns meaningful shared lifecycle or invariants

Do not subclass a service merely because one calculation differs.

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

| Pressure | Prefer | Why |
| --- | --- | --- |
| Few tiny local cases | Local branch | Lowest cognitive overhead |
| Behavior grows independently | Composition | Behavior owns its own rules |
| Behavior selected by business context | Composition + explicit resolver | Selection stays separate |
| Real stable subtype + shared lifecycle/invariants | Inheritance may fit | True is-a relationship |
| Only one step of a larger use case varies | Composition | Vary the part, not the whole service |

## Important lesson

POL-101 was not a bad design that POL-110 repaired.

The problem changed.

~~~text
simple local variation
-> simple local branch

independent behavior growth
-> explicit policy objects
~~~

Architecture should evolve with pressure.

## Final architecture

~~~text
CancellationTerms
        |
        v
CancellationPolicyResolver
        |
        v
CancellationPolicy
    /           \
Flexible       Strict
Policy         Policy
        ^
        |
CancellationService
~~~

## What this skeleton teaches

- local branching is sometimes the clearest design
- abstraction should be justified by independent behavior growth
- composition can vary one part without subclassing the whole service
- policy implementation and policy selection are different concerns
- explicit wiring can be clearer than framework magic
- inheritance needs a real subtype relationship
- fewer classes is not always simpler
- more abstractions is not automatically better

## Deliberately not included

This module does not add:

- Spring
- persistence
- HTTP
- Kafka
- reflection registries
- configuration-driven generic rules
- rule engines
- inheritance examples created only for demonstration

Those would introduce different pressures.

## Run

~~~bash
./gradlew test
~~~

## Freeze rule

This module is intentionally closed at **1.0.0**.

A new architecture pressure belongs in a new skeleton.

## Next

**06 - Aggregate / Ownership**

Dominant question:

> Which objects must change together to protect one business invariant, and where should the consistency boundary live?
