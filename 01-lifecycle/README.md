# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.11.0-SNAPSHOT - Operability / Production Baseline**

The service now exposes health probes, Prometheus metrics, structured logs, request correlation IDs, and OpenTelemetry tracing support.

## Evolution

- **0.1.0 - Core Order Lifecycle**: entity-owned state transitions demonstrated from a plain Java `main()`.
- **0.2.0 - Application Use Cases**: application boundary and repository port.
- **0.3.0 - HTTP Order API**: Spring Boot REST API.
- **0.4.0 - Durable Order Persistence**: PostgreSQL, Flyway, JPA, Testcontainers.
- **0.5.0 - Concurrent Transition Protection**: optimistic locking and transaction boundaries.
- **0.6.0 - Idempotent Order Creation**: persisted retry ownership.
- **0.7.0 - Lifecycle Audit Trail**: durable committed history.
- **0.8.0 - Domain Events**: named business facts.
- **0.9.0 - Transactional Outbox**: atomic state and event persistence.
- **0.10.0 - Reliable Kafka Publication**: lease-based at-least-once relay.
- **0.11.0-SNAPSHOT - Operability / Production Baseline**: probes, metrics, structured logs, correlation, and tracing.

## Production signals

Health:

~~~text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
~~~

Metrics:

~~~text
/actuator/prometheus
~~~

Important relay signals:

~~~text
order.outbox.relay.published
order.outbox.relay.failed
order.outbox.backlog
order.outbox.oldest.unpublished.age
~~~

Readiness includes PostgreSQL.

Kafka availability does not gate readiness because the transactional outbox allows the synchronous API to continue while delivery is temporarily degraded.

## Request diagnostics

Every HTTP response includes:

~~~text
X-Correlation-Id
~~~

Incoming values are preserved; otherwise the service generates one and places it in MDC for structured logs.

OTLP trace export can be enabled with:

~~~text
OTEL_EXPORT_ENABLED=true
OTEL_TRACES_ENDPOINT=http://collector:4318/v1/traces
~~~

## Run locally with Podman

~~~bash
podman compose up -d
gradle bootRun
~~~

## Final remaining work before 1.0

1. **ORD-210 - Read Performance Optimization**: database-first analysis followed by justified Redis cache-aside.
2. **Deployment baseline**: container image, Kubernetes manifests, and AWS target architecture.
3. Freeze the lifecycle skeleton at **1.0.0** and move to **02-reservation**.
