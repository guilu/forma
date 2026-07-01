# ADR-008: Observability and Logging

## Status

Accepted

## Context

FORMA will include background synchronization, external providers and personal data workflows. Debugging must be possible without exposing sensitive information.

## Decision

FORMA will use structured logging, correlation IDs, health/readiness endpoints, metrics and audit events for sensitive actions.

## Consequences

- Production issues can be traced through correlation IDs.
- Background jobs can be monitored.
- Provider sync failures can be diagnosed.
- Sensitive data requires explicit logging rules.

## Rules

- Every backend request should have a correlation ID.
- Do not log passwords, access tokens, refresh tokens or provider secrets.
- Do not log full personal health payloads.
- Health endpoints must not expose sensitive configuration.
- Metrics must avoid high-cardinality personal labels.
- Audit logs record sensitive actions without storing sensitive payloads.
