# ADR-002: Authentication and Authorization

## Status

Accepted

## Context

FORMA handles personal health, training, nutrition and integration data. Even if the first deployment is personal or single-user, the system must not be designed as if authorization does not matter.

## Decision

FORMA will implement production-ready authentication and server-side authorization from the MVP platform layer.

Authentication identifies the user. Authorization verifies whether that user can access or mutate a specific resource.

## Consequences

- Every protected backend endpoint must resolve the current account.
- Data ownership checks must happen server-side.
- Frontend checks are usability helpers, not security controls.
- Tests must cover cross-user access attempts.

## Rules

- Do not rely on UI filtering for security.
- Do not expose provider tokens to the frontend.
- Do not log credentials, access tokens or refresh tokens.
- Reject unauthenticated requests to protected resources.
- Reject cross-user reads and writes.

## Open points

The concrete provider/framework may be selected during implementation, but it must support secure session or token lifecycle management and local development configuration.
