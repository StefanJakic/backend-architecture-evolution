# Kubernetes Deployment Baseline

## Goal

The Kubernetes manifests demonstrate the runtime contract of the application without turning this repository into a platform repository.

Apply the baseline with:

~~~bash
kubectl apply -k k8s/base
~~~

The image reference is:

~~~text
ghcr.io/stefanjakic/order-lifecycle:1.0.0
~~~

Replace the registry or tag in the deployment process when needed.

## Required runtime secret

The Deployment expects a Kubernetes Secret named:

~~~text
order-lifecycle-runtime
~~~

with these keys:

~~~text
db-url
db-username
db-password
kafka-bootstrap-servers
redis-url
~~~

No credential values are committed to this repository.

In a real AWS environment, these values should be injected from a managed secret source rather than maintained manually in Git.

## Replicas and rolling updates

The baseline starts with:

~~~text
replicas: 2
maxUnavailable: 0
maxSurge: 1
~~~

This demonstrates a rolling update that keeps existing capacity while bringing up a replacement Pod.

It is a starting point, not a sizing result.

Replica count, requests, and limits must be adjusted from load-test and production metrics.

## Probes

### Startup

~~~text
/actuator/health/liveness
~~~

The startup probe gives the JVM and Spring context up to roughly one minute to become live before Kubernetes starts normal liveness/readiness handling.

### Liveness

~~~text
/actuator/health/liveness
~~~

Liveness answers whether restarting this process can help.

It intentionally does not represent PostgreSQL, Kafka, or Redis availability.

### Readiness

~~~text
/actuator/health/readiness
~~~

Readiness includes PostgreSQL because the Order API cannot serve correct synchronous reads/writes without its source-of-truth database.

Kafka is not part of readiness because the transactional outbox allows the HTTP write path to continue while Kafka delivery is temporarily degraded.

Redis is not part of readiness because status reads fail open to PostgreSQL.

## Graceful shutdown

The Pod allows:

~~~text
terminationGracePeriodSeconds: 30
~~~

Spring is configured for graceful shutdown with a bounded shutdown phase.

When Kubernetes terminates a Pod, Service routing stops treating the terminating Pod as a normal ready endpoint while the application gets time to finish in-flight work.

## Security baseline

The container:

- runs as UID/GID 10001
- is non-root
- cannot gain additional privileges
- drops Linux capabilities
- uses the runtime-default seccomp profile
- does not mount the Kubernetes service-account token

These controls are appropriate because the application does not call the Kubernetes API.

## Resources

The checked-in requests/limits are starter values only:

~~~text
requests:
  cpu: 250m
  memory: 512Mi

limits:
  cpu: 1
  memory: 1Gi
~~~

They must be tuned using actual JVM, traffic, database, Kafka, and relay metrics.

## PodDisruptionBudget

The baseline keeps at least one application Pod available during voluntary disruptions.

This is useful with two replicas but does not replace proper multi-AZ scheduling, capacity planning, or failure testing.

## Deliberate omissions

This skeleton does not include:

- Helm
- HorizontalPodAutoscaler
- Ingress controller installation
- service mesh
- External Secrets controller
- OpenTelemetry Collector deployment
- Prometheus operator
- Terraform

Those are platform concerns that can vary by organization.

The application manifests exist to make the application runtime contract concrete.
