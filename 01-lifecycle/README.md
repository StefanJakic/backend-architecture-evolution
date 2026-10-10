# 01 - Lifecycle / State Machine

This project evolves an Order lifecycle from a small executable domain sketch into a production-oriented backend service.

## Current development version

**0.12.0-SNAPSHOT - Read Performance Optimization**

Current Order status can now use an optional Redis cache-aside layer while PostgreSQL remains the source of truth.

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
- **0.11.0 - Operability / Production Baseline**: probes, metrics, structured logs, correlation, and tracing.
- **0.12.0-SNAPSHOT - Read Performance Optimization**: optional Redis cache-aside for current status.

## Current status read

~~~text
GET /orders/{orderId}
       ↓
Redis status cache
   ↙          ↘
 hit          miss/error
 ↓                ↓
return         PostgreSQL
                   ↓
                cache
                   ↓
                return
~~~

PostgreSQL remains authoritative.

Redis does not participate in:

- lifecycle transitions
- optimistic locking
- idempotency
- audit
- outbox
- Kafka publication

## Cache consistency

A successful lifecycle transition invalidates the cached status after the PostgreSQL transaction commits.

Entries also have a finite TTL, defaulting to 5 minutes.

Redis failures fall back to PostgreSQL and do not fail Order requests.

## Cache metrics

~~~text
order.status.cache.requests{result=hit|miss}
order.status.cache.errors{operation=read|write|invalidate}
~~~

## Run locally with Podman

~~~bash
podman compose up -d
gradle bootRun
~~~

The local environment starts:

~~~text
PostgreSQL
Kafka
Redis
~~~

Cache settings:

~~~text
ORDER_STATUS_CACHE_ENABLED=true
ORDER_STATUS_CACHE_TTL=5m
REDIS_URL=redis://localhost:6379
~~~

## Final remaining work

One final feature remains for this skeleton:

**ORD-220 - Deployment Baseline / 1.0 Release**

- container image
- Kubernetes manifests
- AWS target architecture
- production configuration notes
- freeze `01-lifecycle` at `1.0.0`

After that, development moves to **02-reservation / time-window concurrency**.
