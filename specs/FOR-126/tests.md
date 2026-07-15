# FOR-126 Test Plan

Strict TDD: failing tests first at each layer (domain → application → persistence → API), then implement. Token-leak assertions are first-class even though no tokens exist yet (guards the port for slices 2-3).

## Scope

Provider-neutral connection domain, status read model, and connect/disconnect/manual-sync commands that resolve. NO OAuth, NO tokens, NO real Withings sync (later slices).

## Domain Tests

- `IntegrationConnection` state transitions: DISCONNECTED → CONNECTED (connect) → DISCONNECTED (disconnect).
- Connect sets `connectedAt`; disconnect clears/ends the connection.
- Sync records `lastSyncAt` + a `SyncOutcome`; `importedCount` defaults to 0 and is never fabricated.
- Connect-when-connected and disconnect-when-disconnected follow the documented idempotent-or-409 decision.

## Application Tests

- Status read model lists all known providers, defaulting to DISCONNECTED when none stored.
- Connect/disconnect/sync go through the application port; the port exposes no token accessor (compile-time + review).
- Owner-scoping: connections are owner-scoped.

## Persistence Tests

- Round-trip a connection (+ last-sync outcome) through the JDBC adapter against H2-in-PostgreSQL-mode with Flyway (V12).
- Empty DB → all providers DISCONNECTED, no error.

## API Tests

- `GET /integrations` before any connection → 200, all providers DISCONNECTED, never 404.
- `POST /{provider}/connect` → 200, status CONNECTED, `connectedAt` set; subsequent GET reflects it.
- `POST /{provider}/sync` on a connected provider → returns a real outcome (`importedCount: 0` acceptable); updates `lastSyncAt`.
- `DELETE /{provider}` → status DISCONNECTED on subsequent GET.
- Unknown `provider` → 400 `VALIDATION_ERROR`.
- Sync-when-disconnected / connect-when-connected → the documented 409-or-idempotent behavior.
- **No response body or log line contains a token/secret** (assert; guards the contract for slices 2-3).

## Edge Cases

- Repeated connect → idempotent or 409 per decision.
- Repeated disconnect → idempotent no-op.
- Sync on disconnected provider → documented behavior, no crash.

## Fixtures

- A stored connection in each state to exercise status transitions.
- H2-in-PostgreSQL-mode with Flyway migrations for persistence/API integration tests, matching FOR-107/110/125 test style.
