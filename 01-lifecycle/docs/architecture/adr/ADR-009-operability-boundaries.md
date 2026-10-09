# ADR-009 - Separate liveness, readiness, and delivery health

## Status

Accepted

## Context

The service depends on PostgreSQL for synchronous API correctness and Kafka for asynchronous external delivery.

Treating every dependency failure as the same kind of health failure can cause harmful platform behavior.

## Decision

Use separate production signals:

- liveness: internal process health only
- readiness: application availability plus PostgreSQL
- Kafka delivery: outbox backlog, age, and failure metrics

Do not make Kafka availability a readiness requirement.

## Rationale

The transactional outbox intentionally decouples the HTTP write path from Kafka availability.

When Kafka is unavailable:

~~~text
HTTP request
  ↓
PostgreSQL transaction
  ↓
outbox PENDING
  ↓
request can still succeed
~~~

The correct operational signal is therefore a growing outbox backlog, not an unhealthy HTTP instance.

## Consequences

### Positive

- Kafka outages do not trigger unnecessary pod restarts or traffic removal.
- PostgreSQL outages stop instances from receiving traffic they cannot serve.
- relay degradation is visible through dedicated metrics.
- operators can alert on backlog age independently from API health.

### Negative

- health is represented by multiple signals instead of one UP/DOWN flag.
- alerting must understand asynchronous delivery health.
- a service can be HTTP-ready while Kafka delivery is degraded by design.
