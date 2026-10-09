# ORD-200 - Operability / Production Baseline

## Goal

A production-oriented service must be diagnosable while it is running.

Correct business logic is not enough if operators cannot answer:

- Is the process alive?
- Is it ready to receive traffic?
- Is event delivery falling behind?
- Are Kafka publications failing?
- Which logs belong to one request?
- Can a request be traced across boundaries?

## Actuator endpoints

The service exposes:

~~~text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/metrics
/actuator/prometheus
~~~

## Liveness vs readiness

Liveness answers:

> Can this process recover by itself, or should the platform restart it?

It deliberately does not depend on PostgreSQL or Kafka.

If Kafka is down and all replicas fail liveness because of it, Kubernetes may restart every healthy process and make the outage worse.

Readiness answers:

> Can this instance currently serve its API correctly?

PostgreSQL is included in readiness because Order reads and writes cannot work correctly without it.

Kafka is deliberately not included.

The transactional outbox means the HTTP write path can keep committing durable events while Kafka is temporarily unavailable.

## Outbox metrics

Custom Micrometer metrics include:

~~~text
order.outbox.relay.published
order.outbox.relay.failed
order.outbox.backlog{status=pending}
order.outbox.backlog{status=in_progress}
order.outbox.oldest.unpublished.age
~~~

Backlog size shows queue depth.

Oldest unpublished age is often more useful operationally because a small number of very old events can still indicate a stuck relay.

## Structured logs

Console logs use Spring Boot structured Logstash JSON.

The relay logs include structured fields such as:

~~~text
eventId
orderId
eventType
attemptCount
~~~

This makes logs queryable without parsing free-form text.

## Correlation ID

HTTP requests accept:

~~~text
X-Correlation-Id
~~~

If the caller supplies one, the service preserves it.

If not, the service generates one.

The value is:

- returned in the response
- placed in SLF4J MDC
- automatically included in structured JSON logs

Tracing and correlation IDs solve related but different problems: trace context links distributed spans, while an explicit correlation ID remains convenient for support and API callers.

## Tracing

Spring Boot OpenTelemetry support is enabled through the official starter.

OTLP export is disabled by default for local development.

Enable it with:

~~~text
OTEL_EXPORT_ENABLED=true
OTEL_TRACES_ENDPOINT=http://collector:4318/v1/traces
~~~

Sampling defaults to 10% and can be changed through:

~~~text
TRACING_SAMPLING_PROBABILITY
~~~

## Graceful shutdown

The application uses graceful shutdown with a bounded shutdown phase.

This gives in-flight HTTP work time to finish while the instance transitions out of readiness.

## What this does not do

This feature makes production signals available.

It does not yet provide:

- Grafana dashboards
- alert rules
- Kubernetes manifests
- cloud infrastructure

Those belong to the final deployment baseline rather than application business code.
