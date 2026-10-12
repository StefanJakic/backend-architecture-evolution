# WFL-140 - Workflow Skeleton 1.0 Freeze

## Status

Complete.

## Scope

Freeze the workflow/orchestration learning skeleton at **1.0.0**.
This change updates release metadata and documentation without changing runtime behavior.

## Completed pressures

| Feature | Architectural decision |
| --- | --- |
| WFL-101 | One explicit orchestrator owns reserve -> charge -> shipment |
| WFL-110 | Partial failure triggers explicit inverse business actions |
| WFL-120 | PostgreSQL checkpoints store progress and IDs; a new orchestrator can resume |
| WFL-130 | Stable operation keys make uncertain forward replay safe when the downstream capability honors its idempotency contract |

## Supported contract

With one active runner for a workflow:

1. Each forward step runs in the defined order.
2. A successful local checkpoint stores the phase and returned ID.
3. Resume skips checkpointed steps.
4. COMPLETED returns the stored result without additional side effects.
5. A successful remote call without a local checkpoint may be invoked again with the same operation key.
6. The downstream capability owns deduplication and returning the original result.
7. Payment failure attempts inventory release; shipment failure attempts refund followed by release.
8. Compensation failures remain suppressed under the original failure.
9. If the compensation checkpoint succeeds, COMPENSATED and COMPENSATION_FAILED prohibit forward resume.

No local transaction spans the external operations.

## Existing acceptance evidence

| Test | Evidence |
| --- | --- |
| completedWorkflowReturnsStoredResultWithoutRepeatingSideEffects | Ordered execution followed by stored-result replay |
| paymentFailureCompensatesReservationAndCheckpointsOutcome | Release inventory and record compensation |
| shipmentFailureCompensatesInReverseOrder | Refund before inventory release |
| compensationFailureKeepsOriginalFailurePrimaryAndDurable | Original exception stays primary; compensation failures are suppressed; outcome recorded |
| resumesFromPersistedPaymentCheckpointAfterProcessCrash | New orchestrator reads PAYMENT_CHARGED and only creates shipment |
| replaysPaymentSafelyWhenCrashHappensBeforePaymentCheckpoint | Two payment requests, one logical charge, original payment ID, final COMPLETED |

The two crash tests use PostgreSQL and a newly instantiated orchestrator.
The payment fake retains its in-memory deduplication state across the simulated orchestrator restart.
Provider restart durability and reserve/shipment deduplication are contracts still requiring real adapters and their tests.

## Boundaries of 1.0

- Forward replay safety depends on durable, atomic provider idempotency; transport may deliver requests more than once.
- Conditional database transitions detect a stale phase but do not coordinate multiple active runners.
- Payment/shipment RuntimeExceptions currently trigger compensation. Production timeout handling must distinguish unknown outcomes from definitive rejection and reconcile uncertain results.
- Refund/release lack idempotency keys and durable per-action compensation checkpoints.
- A crash during compensation, or failure to record its terminal outcome, requires reconciliation.
- No automatic recovery scanner, retry/backoff scheduler, or operator repair mechanism is included.
- Real provider adapters and production observability are outside this skeleton.

## Why freeze now?

The module answers its dominant question: sequencing, local progress, recovery, and compensation belong to the orchestrator; replay deduplication belongs to the external operation boundary.

Further resilience work introduces a separate dominant pressure rather than another orchestration abstraction.

## Release verification

- Build version and README version agree on 1.0.0.
- Repository progress marks 07 complete and 08 next.
- Runtime source, schema, dependencies, and tests remain unchanged.
- The user reported the WFL-130 test suite passed before this freeze.
- This documentation and version change does not claim a new local Gradle execution.

## Next pressure

**08 - External Boundary / Adapter**

How should an application isolate an unreliable external dependency, translate its outcomes, and decide which failures are safe to retry?
