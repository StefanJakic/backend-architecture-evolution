# Backend Architecture Evolution

A hands-on engineering workspace that evolves backend systems from small domain sketches into production-oriented Spring Boot architectures.

The repository is intentionally developed feature by feature. Git history is part of the documentation: requirements explain **what** is needed, tests capture acceptance criteria, ADRs explain **why** an architectural decision was made, and commits show **how** the implementation evolved.

## Engineering workflow

```text
Requirement
  -> Issue / feature specification
  -> Feature branch
  -> Tests and implementation
  -> ADR when an architectural trade-off is introduced
  -> Pull request
  -> Merge to main
  -> Versioned release
```

## Planned architecture families

1. Lifecycle / State Machine
2. Reservation / Time Window
3. Scarce Resource / Inventory
4. Assignment / Matching
5. Behavior / Policy
6. Aggregate / Ownership
7. Workflow / Orchestration
8. External Boundary / Adapter
9. Event / Reaction
10. Hierarchy / Tree
11. Rule / Validation Pipeline

## Evolution principle

Each project begins with the smallest executable model that demonstrates the business invariant, usually a framework-free Java `main()`. Spring Boot, persistence, concurrency control, messaging, observability, containers, and deployment concerns are added only when a requirement creates pressure for them.

## Commit convention

The first line stays concise and production-like, while the commit body records the learning context and architectural reasoning.

Example:

```text
feat(lifecycle): guard order transitions - protect the core invariant

Keep lifecycle rules inside Order instead of allowing callers to mutate
status directly.

Why:
- invalid transitions must be impossible
- the entity should own its lifecycle rules
- no framework is needed to demonstrate this invariant yet
```
