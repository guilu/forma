# FORMA Architecture Overview

## Purpose

FORMA is a personal health and fitness planning application for tracking body composition, planning training, planning nutrition, generating shopping support and producing explainable insights.

The architecture must support human development and AI-assisted development without turning the codebase into a pile of clever shortcuts.

## Architectural style

FORMA follows hexagonal architecture with domain-driven design principles.

```text
UI / API
  -> Application use cases
    -> Domain model
    -> Ports
      -> Adapters: persistence, providers, scheduling, notifications
```

## Main bounded contexts

- Foundation
- Body Composition
- Training Engine
- Nutrition Planner
- Shopping Assistant
- Insights Engine
- External Integrations
- UI & UX
- Platform & Infrastructure

## Dependency direction

Dependencies point inward.

- Domain does not depend on frameworks.
- Application layer depends on domain and ports.
- Adapters depend on application/domain contracts.
- API and UI orchestrate interaction but do not own business rules.

## Backend responsibilities

- Domain rules.
- Use cases.
- Persistence orchestration.
- Authorization enforcement.
- Provider synchronization.
- Insight generation.
- Audit and observability.

## Frontend responsibilities

- Navigation and layout.
- Rendering read models.
- Collecting user input.
- Triggering commands.
- Displaying validation, loading, empty and error states.

The frontend must not duplicate domain calculations such as body composition derivations, training progression rules or nutrition macro rules.

## Data ownership

All personal data belongs to a user/account boundary. Server-side authorization must enforce ownership checks even if the MVP starts as a single-user deployment.

## Integration model

External providers such as Withings, Garmin, Apple Health or Health Connect are adapters. The core domain should use normalized measurements and activities, not provider-specific payloads.

## Documentation model

Global decisions live under `docs/`.

Story-specific implementation guidance will live under:

```text
specs/FOR-XXX/
```

Agent-focused shared context lives under:

```text
.ai/
```

## Non-goals for MVP

- Medical diagnosis.
- Fully automated clinical advice.
- Multi-tenant enterprise administration.
- Real-time coaching.
- Provider marketplace.
