# ASG-101 - Eligibility and Deterministic Selection

## Dominant problem

Assignment is not only about whether work can be done.

It is also about choosing **which** candidate should receive or claim the work.

The first local question is:

> Given one worker and several jobs, which job should be selected next?

## Model

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
worker.capability
==
job.requiredCapability
~~~

## Selection order

The selector applies:

~~~text
1. eligible jobs only
2. highest priority first
3. lowest job ID as stable tie-breaker
~~~

The ID tie-breaker is not a business recommendation by itself.

It exists so identical inputs always produce one deterministic result.

## Why deterministic selection matters

Without a stable order:

~~~text
same inputs
-> different local choices
~~~

which makes behavior harder to test and reason about.

With a stable order:

~~~text
Scheduler A sees same jobs -> Job-1
Scheduler B sees same jobs -> Job-1
~~~

Both schedulers can still make the **same correct local decision**.

That is exactly the next concurrency problem.

## What this does not solve

Selection does not imply ownership.

This is safe:

~~~text
select best candidate
~~~

only as a pure decision.

It is not sufficient for:

~~~text
select
then claim exclusively
~~~

when multiple schedulers share the same database state.

## Next pressure

ASG-110 will persist jobs and deliberately separate:

~~~text
SELECT next pending job
    ↓
UPDATE job as claimed
~~~

Two transactions will be coordinated so they both select the same pending job before either claim is written.

That will prove:

~~~text
deterministic selection
!=
exclusive claiming
~~~
