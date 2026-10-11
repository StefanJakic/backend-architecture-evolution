# ORD-220 - Deployment Baseline and 1.0 Release

## Goal

Close the lifecycle skeleton after proving the full evolution from a small domain model to a production-oriented backend service.

This feature does not add new business behavior.

It packages and documents the system that already exists.

## Container

The Docker build uses two stages:

~~~text
Java 21 JDK
  -> Gradle bootJar

Java 21 JRE
  -> app.jar
  -> non-root UID/GID 10001
~~~

The runtime image does not contain the Gradle build toolchain.

## Kubernetes

The baseline includes:

- Deployment
- ClusterIP Service
- ConfigMap
- PodDisruptionBudget
- Kustomize entry point

Runtime behavior includes:

- two replicas
- rolling update
- startup probe
- liveness probe
- readiness probe
- graceful termination window
- non-root process
- dropped Linux capabilities
- resource requests/limits
- external secret references

No credentials are stored in Git.

## Health semantics carried into deployment

~~~text
PostgreSQL failure
  -> readiness DOWN

Kafka failure
  -> Pod stays ready
  -> outbox backlog grows
  -> relay retries

Redis failure
  -> Pod stays ready
  -> status reads fall back to PostgreSQL
~~~

The deployment layer preserves application architecture rather than redefining it.

## AWS target

The documented target is:

~~~text
ALB
  ↓
EKS
  ├── RDS PostgreSQL Multi-AZ
  ├── Amazon MSK
  ├── ElastiCache Redis/Valkey
  └── OpenTelemetry / Prometheus path

ECR -> application image
Secrets Manager -> runtime secrets
~~~

The repository does not provision these resources.

## Why no Terraform or Helm?

The purpose of this repository is backend architecture evolution.

A complete AWS/EKS platform would introduce a different set of concerns:

- VPC design
- IAM
- cluster add-ons
- DNS/certificates
- autoscaling
- secret operators
- deployment pipelines
- backup/DR automation

Those are valid topics but would make this skeleton unnecessarily large.

## Version 1.0

Version `1.0.0` means the lifecycle learning objective is complete.

It does not mean every possible production concern has been implemented.

It means the skeleton now demonstrates the intended backend architecture pressures end-to-end and is intentionally frozen.

## Final evolution

~~~text
0.1  Domain lifecycle
0.2  Application use cases
0.3  HTTP API
0.4  PostgreSQL durability
0.5  Optimistic locking
0.6  Idempotency
0.7  Audit trail
0.8  Domain events
0.9  Transactional outbox
0.10 Kafka relay / at-least-once
0.11 Operability
0.12 Redis cache-aside
1.0  Deployment baseline + freeze
~~~

## Next skeleton

Development moves to:

**02-reservation / time-window concurrency**

That skeleton is intentionally smaller and focuses on a different dominant problem:

~~~text
time ranges
overlap
concurrent booking
locking / database constraints
temporary holds
expiry
~~~
