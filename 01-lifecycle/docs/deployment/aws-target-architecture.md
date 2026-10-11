# AWS Target Architecture

## Purpose

This document maps the completed lifecycle application to a realistic AWS production environment.

It is a target architecture, not infrastructure-as-code.

## High-level view

~~~text
Internet
   |
   v
Application Load Balancer
   |
   v
Amazon EKS
+----------------------------------+
| order-lifecycle Pods             |
|  - HTTP API                      |
|  - outbox relay                  |
|  - Redis cache-aside client      |
|  - Prometheus / OTLP signals     |
+----------------------------------+
   |             |             |
   |             |             |
   v             v             v
RDS PostgreSQL   Amazon MSK    ElastiCache
source of truth  Kafka         Redis/Valkey
   |
   v
durable Order + audit + outbox

Container images:
ECR -> EKS

Secrets:
Secrets Manager -> runtime secret injection

Observability:
Prometheus scrape / collector
OTLP traces -> OpenTelemetry Collector -> chosen backend
structured stdout logs -> cluster logging pipeline
~~~

## Amazon EKS

The application runs as multiple Kubernetes Pods in private subnets.

The checked-in Deployment demonstrates two replicas and rolling updates.

A production cluster should distribute compute across Availability Zones and use the organization’s normal EKS node or Auto Mode strategy.

The application does not require Kubernetes API access, so the Pod does not mount a service-account token.

## Edge traffic

An Application Load Balancer is the expected public HTTP entry point.

The repository intentionally does not install or configure the AWS Load Balancer Controller because that belongs to the EKS platform layer.

The application Service remains ClusterIP.

## RDS PostgreSQL

PostgreSQL remains the source of truth for:

- current Order state
- optimistic-lock version
- idempotency ownership/result
- audit history
- transactional outbox
- outbox delivery state

The production target is RDS for PostgreSQL with Multi-AZ availability.

This is the dependency that participates in application readiness.

A PostgreSQL outage means the synchronous Order API cannot safely serve reads/writes.

## Amazon MSK

Amazon MSK is the target Kafka service.

The application publishes durable outbox events to Kafka with at-least-once delivery.

For a high-availability production cluster, use brokers across multiple Availability Zones and configure clients with multiple bootstrap brokers.

Topic durability should match the availability requirement; a three-AZ setup commonly uses replication factor 3 with min in-sync replicas below the replication factor so one replica can be unavailable without stopping all writes.

Kafka availability is not an HTTP readiness dependency.

When Kafka is unavailable:

~~~text
Order write
  -> PostgreSQL COMMIT
  -> outbox remains PENDING
  -> API can succeed
  -> backlog/failure metrics rise
  -> relay retries later
~~~

## ElastiCache

ElastiCache for Redis OSS or Valkey is the target cache service.

It stores only current Order status.

It is not authoritative and does not participate in Order write correctness.

A Multi-AZ deployment with automatic failover is appropriate when cache availability matters, but the application still fails open to PostgreSQL if Redis is unavailable.

Redis therefore does not participate in Kubernetes readiness.

## Secrets

Runtime credentials should come from AWS Secrets Manager through the organization’s supported Kubernetes secret-injection mechanism.

The Deployment references a Kubernetes Secret contract but the repository contains no credential values.

Typical secret material includes:

- PostgreSQL URL / credentials
- MSK bootstrap information when treated as restricted environment configuration
- Redis endpoint when managed as restricted runtime configuration
- TLS/authentication material when enabled

## Container registry

Build the Docker image in CI and push it to Amazon ECR.

Deploy immutable version or digest references.

Do not build application images inside the Kubernetes cluster.

## Observability

The application already exposes:

~~~text
/actuator/prometheus
~~~

and supports OTLP tracing.

A typical AWS deployment can route:

~~~text
Prometheus metrics
  -> Prometheus-compatible collector/backend

OTLP traces
  -> OpenTelemetry Collector
  -> organization-selected trace backend

structured stdout logs
  -> cluster log agent
  -> centralized log backend
~~~

The important application-level alerts are not only CPU/memory.

Also watch:

- HTTP error/latency signals
- PostgreSQL availability
- outbox pending count
- oldest unpublished-event age
- relay failures
- cache errors
- cache hit/miss ratio

## Network boundaries

Recommended topology:

~~~text
public subnets
  -> ALB

private application subnets
  -> EKS workload

private data subnets
  -> RDS
  -> MSK
  -> ElastiCache
~~~

Security groups should permit only the required application-to-data-service paths.

## Availability semantics

The system intentionally has different failure behavior for different dependencies:

| Dependency | If unavailable | API readiness |
| --- | --- | --- |
| PostgreSQL | synchronous correctness unavailable | DOWN |
| Kafka / MSK | outbox accumulates and retries | remains UP |
| Redis / ElastiCache | reads fall back to PostgreSQL | remains UP |
| tracing backend | traces are degraded | remains UP |
| metrics backend | scraping/export degraded | remains UP |

That distinction is a direct consequence of the architecture built through this skeleton.

## What would come next in a real platform project?

Outside this skeleton:

- Terraform / CloudFormation / CDK
- EKS platform add-ons
- ECR pipeline
- certificate / DNS management
- external secret controller or CSI integration
- autoscaling
- network policies
- backup / restore testing
- disaster recovery
- deployment promotion strategy
- SLOs and alert rules

Those are valid production concerns, but they are intentionally not expanded inside the lifecycle learning skeleton.
