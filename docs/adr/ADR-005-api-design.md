# ADR-005: REST API Design

## Status

Accepted

## Context

FORMA needs a stable API between frontend and backend. AI agents will implement endpoints over time, so consistency matters more than cleverness.

## Decision

FORMA will expose a versioned REST API for the MVP.

APIs should distinguish commands from queries conceptually, even when both are exposed through REST endpoints.

## Consequences

- API routes must follow a consistent versioning strategy.
- Errors must be predictable and safe.
- Read models should be shaped for UI usage.
- Backend remains responsible for validation and business rules.

## Rules

- Use explicit API versioning.
- Use consistent error response shapes.
- Do not expose stack traces to clients.
- Do not leak internal persistence structures directly as public API contracts.
- Validate all input at API boundaries.
- Prefer clear names over generic endpoints.

## Error handling

Errors should be useful to users and developers without exposing secrets or implementation details.
