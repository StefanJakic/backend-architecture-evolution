# 07 - Workflow / Orchestration

## Status

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This focused skeleton studies multi-step business operations, partial failure, compensation, resumability, and safe replay.

## Dominant question

> When one business operation spans several independent steps, who owns sequencing, failure handling, recovery, and replay safety?

## WFL-101 - Explicit orchestration

~~~text
reserve inventory
    ↓
charge payment
    ↓
create shipment
~~~

FulfillmentWorkflow owns the order.

Each capability owns only its local operation.

## WFL-110 - Explicit compensation

~~~text
payment fails
-> release inventory

shipment fails
-> refund payment
-> release inventory
~~~

Compensation is an inverse business action, not database rollback.

## WFL-120 - Durable progress

Workflow progress survives process restart:

~~~text
STARTED
    ↓
INVENTORY_RESERVED
    ↓
PAYMENT_CHARGED
    ↓
COMPLETED
~~~

The database stores the IDs returned by completed steps.

A restarted process loads the durable phase and skips already checkpointed steps.

## WFL-130 - Idempotent replay boundary

Durable state alone cannot answer whether a remote side effect succeeded immediately before a crash:

~~~text
charge payment SUCCESS
process CRASH
before PAYMENT_CHARGED checkpoint
~~~

After restart the durable phase is still INVENTORY_RESERVED, so payment is called again.

Every forward step now receives a deterministic operation key:

~~~text
order-123:reserve
order-123:charge
order-123:shipment
~~~

A retry of the same logical step reuses exactly the same key.

The external boundary can therefore return the original result instead of repeating the business side effect.

## Request attempts vs business side effects

A safe replay can legitimately look like:

~~~text
payment requests = 2
logical charges  = 1
~~~

The orchestrator is allowed to retry an uncertain call.

The downstream boundary owns deduplication for the stable operation key.

## Crash regression

The integration test proves:

~~~text
reserve SUCCESS
checkpoint INVENTORY_RESERVED

charge(order-1:charge) SUCCESS
PROCESS CRASH
before PAYMENT_CHARGED checkpoint

restart
charge(order-1:charge) again
-> same paymentId
-> no second logical charge

checkpoint PAYMENT_CHARGED
create shipment
checkpoint COMPLETED
~~~

## No long transaction

There is deliberately no database transaction spanning remote calls.

~~~text
remote side effect
→ DB checkpoint
→ remote side effect
→ DB checkpoint
~~~

A local transaction cannot provide atomicity across independent external systems.

## Important boundary

Idempotency is not the same as exactly-once transport.

~~~text
request delivery:       may happen more than once
business side effect:   at most once per operation key
                        when the provider honors its idempotency contract
~~~

The external capability must durably remember the operation key and its original result in a real distributed system.

The test double models that contract in memory.

## Evolution

~~~text
WFL-101  explicit orchestrator owns step order
WFL-110  partial failure + compensation
WFL-120  persist workflow state / resume after crash
WFL-130  idempotent retry boundary
WFL-140  architectural guarantees + 1.0 freeze       <- complete
~~~

## Run

~~~bash
./gradlew test
~~~

## Final architectural contract

The supported recovery model is sequential execution with one active runner per workflow.

| Concern | Owner | Behavior |
| --- | --- | --- |
| Step order | FulfillmentWorkflow | Reserve, charge, then create shipment |
| Local progress | FulfillmentStateRepository | Persist phase and returned IDs after each successful step |
| Resume | FulfillmentWorkflow | Skip checkpointed forward steps and return stored results for COMPLETED |
| Uncheckpointed success | External capability | Replay with the same operation key and return the original result |
| Payment failure | FulfillmentWorkflow | Attempt inventory release |
| Shipment failure | FulfillmentWorkflow | Attempt payment refund, then inventory release |
| Compensation failure | FulfillmentWorkflow | Preserve the original exception; attach compensation failures as suppressed exceptions |
| Terminal compensation outcome | FulfillmentStateRepository | Record COMPENSATED or COMPENSATION_FAILED when the checkpoint succeeds; reject forward resume |

PostgreSQL transitions check the expected prior phase. A rejected transition fails explicitly.
This guard does not provide exclusive ownership of remote operations across competing runners.

## Evidence

Existing tests cover:

- forward step order and stored-result replay after completion
- compensation order and preservation of the primary failure
- persisted PAYMENT_CHARGED recovery without a second reserve or charge call
- payment success followed by a crash before its checkpoint: two requests, one logical charge, original payment ID, and eventual COMPLETED

The crash tests instantiate a new orchestrator over the same PostgreSQL repository.
The payment fake survives that simulated orchestrator restart and keeps its idempotency results in memory.
They do not prove downstream durability across a provider restart or deduplication for reserve and shipment.

## Remaining production concerns

- **Provider idempotency:** Real adapters must deduplicate atomically, persist the original outcome, reject conflicting payloads for a reused key, and retain keys for the required replay period.
- **Ambiguous outcomes:** The current payment/shipment RuntimeException path triggers compensation. A timeout can occur after a provider committed; production adapters need an explicit distinction between rejection and unknown outcome, plus reconciliation before compensation.
- **Compensation recovery:** Refund and release have no operation keys or per-action durable checkpoints. A crash during compensation or a failed compensation checkpoint needs reconciliation; forward replay alone is insufficient.
- **Concurrent execution:** No runner lease or lock is implemented. Conditional DB updates do not prevent conflicting forward calls and compensation by different runners.
- **Recovery scheduling:** Resume is explicitly invoked. There is no background scanner, retry budget, backoff, or recovery worker.
- **Real dependencies and operations:** Production adapters, timeout policies, circuit breakers, metrics, tracing, alerts, and operator repair are separate work.

## Freeze rule

This learning skeleton is closed at **1.0.0** after WFL-101 through WFL-130.
The freeze records the supported contract and its limits; it is not a production-readiness claim.

See [WFL-140 - Workflow Skeleton 1.0 Freeze](docs/features/WFL-140-workflow-1.0-freeze.md).

## Next pressure

**08 - External Boundary / Adapter**

> How should an application isolate an unreliable external dependency, translate its outcomes, and decide which failures are safe to retry?
