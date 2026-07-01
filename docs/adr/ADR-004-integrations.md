# ADR-004: External Integrations

## Status

Accepted

## Context

FORMA will import data from external providers such as Withings and future fitness platforms. Provider APIs, payloads and rate limits change independently from the product domain.

## Decision

External integrations will be implemented as adapters behind provider-neutral application ports.

The core domain will use normalized records such as measurements and activities. Provider payloads must be mapped at the adapter boundary.

## Consequences

- New providers can be added without rewriting body composition or training logic.
- Synchronization can be tested through provider-neutral contracts.
- Provider-specific failure handling remains isolated.

## Rules

- Do not store provider payloads as the primary domain model.
- Do not expose provider tokens to domain services or the frontend.
- Synchronization must be idempotent.
- Duplicate detection is mandatory for imported records.
- Provider rate limits must be respected.
- Sync failures must be observable and user-readable without leaking secrets.
