# ADR-007: Testing Strategy

## Status

Accepted

## Context

FORMA will be implemented incrementally, often with AI assistance. Tests are the main protection against confident but wrong changes.

## Decision

FORMA will use a layered testing strategy.

- Domain tests for business rules.
- Application tests for use cases.
- Adapter tests for persistence and external integrations.
- API tests for request/response behavior.
- Frontend tests for key rendering and interaction states.
- End-to-end tests only for critical flows when useful.

## Consequences

- Domain rules stay fast to verify.
- Integration risk is isolated.
- AI-generated code must prove behavior through tests.

## Rules

- New business rules require tests.
- Bug fixes should include regression tests.
- Avoid relying only on snapshot tests.
- Do not mock the domain model to test the domain model.
- External providers should be tested through contracts/fakes when possible.
- CI must run the relevant test suite.
