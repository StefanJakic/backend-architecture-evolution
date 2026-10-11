# POL-101 - Keep Small Policy Variation Local

## Goal

Start with the smallest readable representation of behavior variation.

The use case is cancellation refund calculation.

Two business terms exist:

~~~text
FLEXIBLE
STRICT
~~~

## Rules

Flexible:

~~~text
cancel >= 24h before start
    -> 100% refund

cancel < 24h before start
    -> 50% refund
~~~

Strict:

~~~text
cancel >= 168h before start
    -> 100% refund

cancel < 168h before start
    -> 0% refund
~~~

## Current design

~~~text
CancellationService
    |
    +-- switch(CancellationTerms)
            |
            +-- FLEXIBLE
            |
            +-- STRICT
~~~

The switch is deliberate.

## Why no policy interface yet?

The variation is currently:

- small
- local
- cohesive
- easy to scan
- easy to test

Introducing:

~~~text
CancellationPolicy
FlexibleCancellationPolicy
StrictCancellationPolicy
PolicyFactory
~~~

would create more structure without solving a real pressure.

The important rule is not:

> branches are bad

It is:

> keep a branch local while it is still the clearest representation of the variation.

## Why this matters

A common design mistake is to identify two branches and immediately build a polymorphic hierarchy.

That can make a tiny use case harder to understand.

Another mistake is the opposite:

~~~text
one switch
    -> more cases
    -> more data dependencies
    -> more helper methods
    -> branch jungle
~~~

The correct abstraction point depends on how the behavior evolves.

## Money simplification

This skeleton uses integer cents.

The 50% example uses even test amounts, so currency rounding rules are intentionally outside scope.

## Next pressure

POL-110 will make the policy behaviors diverge enough that keeping all rules inside one service becomes harder to read.

Only then will an explicit policy abstraction earn its place.
