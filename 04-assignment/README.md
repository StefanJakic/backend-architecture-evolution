# 04 - Assignment / Matching

## Status

**0.1.0-SNAPSHOT - Eligibility + Deterministic Selection**

This focused skeleton studies selection and exclusive claiming.

## Dominant question

> When several pieces of work are eligible, how do we select and claim exactly one without duplicate assignment?

## Current model

~~~text
Worker
  id
  capability

Job
  id
  requiredCapability
  priority
~~~

Eligibility:

~~~text
worker.capability == job.requiredCapability
~~~

Selection:

~~~text
eligible only
    ↓
highest priority
    ↓
stable ID tie-breaker
~~~

## Current lesson

Deterministic selection makes local decisions reproducible.

It does **not** create exclusive ownership.

Two schedulers given the same visible state can both correctly select the same job.

## Planned evolution

~~~text
ASG-101  Eligibility + deterministic selection
ASG-110  PostgreSQL select-then-claim race
ASG-120  SELECT FOR UPDATE SKIP LOCKED
ASG-130  Multi-worker claiming / fairness notes
1.0      Freeze
~~~

## Run

~~~bash
./gradlew test
~~~

## Next pressure

ASG-110 will persist pending jobs and prove:

~~~text
selecting the same best job
+
claiming later
=
duplicate assignment race
~~~
