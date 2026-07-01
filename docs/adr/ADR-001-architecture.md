# ADR-001: Architecture

## Status

Accepted

## Context

FORMA will be developed through small Jira stories and, in many cases, AI-assisted coding agents. The architecture must reduce ambiguity, keep business rules testable and prevent UI/framework code from becoming the product's source of truth.

## Decision

FORMA will use hexagonal architecture with domain-driven design principles.

The main layers are:

- Domain: entities, value objects, domain services and business rules.
- Application: use cases, commands, queries and ports.
- Adapters: persistence, HTTP clients, external providers, schedulers and platform infrastructure.
- Delivery: REST API and frontend.

Dependencies must point inward. Domain code must not depend on frameworks, persistence details or external providers.

## Consequences

- Business rules remain testable without infrastructure.
- External integrations can evolve independently.
- AI agents have clearer boundaries when modifying the codebase.
- Some upfront structure is required, but this avoids expensive rewrites later.

## Rules

- Controllers do not contain business rules.
- UI components do not contain domain calculations.
- Persistence models do not define domain behavior.
- External provider payloads are translated at adapter boundaries.
- Application use cases coordinate domain behavior and ports.
