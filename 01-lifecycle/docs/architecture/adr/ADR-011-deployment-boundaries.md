# ADR-011 - Keep deployment baseline separate from platform provisioning

## Status

Accepted

## Context

The lifecycle skeleton now includes domain modeling, persistence, concurrency, idempotency, events, outbox delivery, observability, and caching.

It needs a concrete production runtime model, but implementing a full cloud platform would change the learning objective from backend architecture to infrastructure engineering.

## Decision

Add:

- a production-style Dockerfile
- a minimal Kubernetes workload baseline
- an AWS target architecture document
- a runtime secret/config contract

Do not add:

- Terraform
- Helm
- EKS add-on installation
- CI/CD implementation
- live AWS resources

## Application-owned concerns

This repository owns:

- container process and user
- HTTP port
- liveness/readiness endpoints
- graceful shutdown
- application environment variables
- resource starting points
- required secret keys
- database/Kafka/Redis failure semantics

## Platform-owned concerns

The deployment environment owns:

- VPC/subnets
- EKS provisioning
- node strategy
- ALB controller
- RDS provisioning
- MSK provisioning
- ElastiCache provisioning
- Secrets Manager integration
- observability backend
- autoscaling policy
- certificate/DNS configuration

## Why this boundary?

The same application can run on different Kubernetes/cloud platforms while preserving its runtime contract.

The application should state what it needs.

The platform should decide how those needs are provisioned and operated.

## Consequences

### Positive

- the repository demonstrates deployability without infrastructure sprawl
- application health semantics remain explicit
- cloud architecture can evolve without changing domain code
- the lifecycle skeleton can be frozen at 1.0

### Negative

- manifests are a baseline, not a complete deployable AWS environment
- environment-specific overlays are still required
- resource values are placeholders until validated by load tests
