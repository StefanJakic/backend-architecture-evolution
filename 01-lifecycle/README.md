# 01 - Lifecycle / State Machine

**Status: COMPLETE / FROZEN**

**Version: 1.0.0**

This flagship skeleton evolves a simple Order lifecycle into a production-oriented backend service.

No additional business features are planned for this module. New learning work moves to the next skeleton instead of expanding this one indefinitely.

## What this skeleton teaches

The dominant problem starts simple:

~~~text
How should an Order move through valid states?
~~~

and gradually creates production pressure around:

~~~text
state ownership
transactions
concurrency
idempotency
auditability
events
durable messaging
at-least-once delivery
operability
caching
deployment
~~~

## Evolution

| Version | Pressure | Main lesson |
| --- | --- | --- |
| 0.1 | lifecycle rules | entity owns valid state transitions |
| 0.2 | use cases | application boundary around domain behavior |
| 0.3 | HTTP | transport stays thin |
| 0.4 | durability | PostgreSQL replaces process memory |
| 0.5 | concurrent writes | optimistic locking rejects stale updates |
| 0.6 | client retries | durable idempotency prevents duplicate creation |
| 0.7 | traceability | append-only lifecycle audit |
| 0.8 | reactions | explicit domain events |
| 0.9 | commit/publish gap | transactional outbox |
| 0.10 | external delivery | Kafka relay with at-least-once semantics |
| 0.11 | production visibility | probes, metrics, logs, correlation, tracing |
| 0.12 | read pressure | Redis cache-aside without moving source of truth |
| 1.0 | runtime | Docker, Kubernetes, AWS target architecture |

## Final architecture

~~~text
                    HTTP
                     |
                     v
                OrderService
                     |
          +----------+----------+
          |                     |
          v                     v
   PostgreSQL                Redis
   source of truth        status cache
          |
          +-- Order state
          +-- optimistic version
          +-- idempotency
          +-- audit
          +-- transactional outbox
                     |
                     v
                Outbox Relay
                     |
                     v
                   Kafka
~~~

## Write path

~~~text
HTTP command
    ↓
@Transactional
    ↓
load Order
    ↓
domain transition
    ↓
persist Order
append audit
append outbox
    ↓
COMMIT
    ↓
invalidate cached status
~~~

The same PostgreSQL transaction owns state, audit, outbox, and creation-idempotency completion.

## Event delivery

~~~text
outbox PENDING
    ↓
claim batch
    ↓
IN_PROGRESS
    ↓
Kafka publish + broker ack
    ↓
PUBLISHED
~~~

If Kafka succeeds but marking the database row as published fails, the event is retried and may be delivered twice.

Therefore:

~~~text
delivery = at-least-once
consumer idempotency key = eventId
~~~

## Read path

~~~text
GET /orders/{id}
      ↓
Redis
  hit | miss/error
      |      ↓
      |  PostgreSQL
      |      ↓
      +---- return
~~~

PostgreSQL remains authoritative.

Redis is disposable acceleration and failures fall back to PostgreSQL.

## Production health model

~~~text
PostgreSQL down
  -> readiness DOWN

Kafka down
  -> API remains ready
  -> outbox backlog grows

Redis down
  -> API remains ready
  -> reads fall back to PostgreSQL
~~~

Important endpoints:

~~~text
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
~~~

## Local runtime

~~~bash
podman compose up -d
./gradlew bootRun
~~~

Local infrastructure:

~~~text
PostgreSQL
Kafka
Redis
~~~

## Tests

~~~bash
./gradlew test
~~~

The integration suite uses Testcontainers for infrastructure behavior.

## Container

Build:

~~~bash
docker build -t order-lifecycle:1.0.0 .
~~~

Run-time configuration is supplied through environment variables.

## Kubernetes

~~~bash
kubectl apply -k k8s/base
~~~

Before deployment, provide the required `order-lifecycle-runtime` secret and use an appropriate image registry/tag.

See:

- `docs/deployment/kubernetes-baseline.md`
- `docs/deployment/aws-target-architecture.md`

## AWS target

The documented production mapping is:

~~~text
ALB
 ↓
EKS
 ├── RDS PostgreSQL Multi-AZ
 ├── Amazon MSK
 └── ElastiCache Redis/Valkey
~~~

with ECR for images, managed secret injection, Prometheus-compatible metrics, structured logs, and OpenTelemetry traces.

## Architecture decisions

The ADRs in `docs/architecture/adr` record why the design evolved instead of only showing the final code.

Important examples:

- optimistic locking rather than silent lost updates
- idempotency in PostgreSQL rather than Redis
- domain events before distributed messaging
- transactional outbox before Kafka
- at-least-once rather than false exactly-once claims
- Kafka degradation separate from HTTP readiness
- cache-aside with PostgreSQL as source of truth
- application runtime contract separate from cloud provisioning

## Freeze rule

This module is intentionally closed at **1.0.0**.

Do not add another technology simply because it can fit.

A new dominant problem belongs in a new skeleton.

## Next

**02 - Reservation / Time Window**

Focus:

~~~text
time overlap
concurrent reservations
database constraints
pessimistic vs optimistic locking
temporary holds
expiry
~~~

Unlike this flagship module, the next skeleton will be much smaller and will stop once those lessons are demonstrated.
