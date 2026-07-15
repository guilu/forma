# FOR-126 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-126
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-103 [STUB] Integrations backend (slice 1 of 3+). Blocks FOR-123.

## Summary

First implementable slice of FOR-103: a **provider-neutral integrations connection
backend**. A connection domain (provider, status, connectedAt, lastSyncAt,
lastSyncOutcome), a status read model, and connect/disconnect/manual-sync endpoints
that actually resolve — replacing the FOR-57 frontend mock and the `Promise<never>`
calls with a real contract. This slice deliberately excludes real OAuth, encrypted
token storage, and real Withings sync — those are FOR-103 slices 2-3. See
`specs/FOR-103/` for the full scope, security requirements, and open questions.

## User/System Flow

1. User opens Integraciones (FOR-57) → `GET /api/v1/integrations` returns per-provider status.
2. User taps "Conectar {provider}" → `POST /api/v1/integrations/{provider}/connect` marks it CONNECTED and resolves.
3. User taps manual sync → `POST /api/v1/integrations/{provider}/sync` records and returns a real `lastSyncOutcome`.
4. User taps "Desconectar" → `DELETE /api/v1/integrations/{provider}` marks it DISCONNECTED and resolves.

## Functional Requirements

- Provider-neutral `IntegrationConnection` domain: `provider` (enum), `status` (CONNECTED/DISCONNECTED), `connectedAt`, `lastSyncAt`, `lastSyncOutcome`. **No tokens in the domain.**
- `GET /api/v1/integrations` — per-provider status list; before any connection every provider is DISCONNECTED.
- `POST /api/v1/integrations/{provider}/connect` — mark CONNECTED (mock, no OAuth), set `connectedAt`, resolve.
- `DELETE /api/v1/integrations/{provider}` — mark DISCONNECTED, resolve.
- `POST /api/v1/integrations/{provider}/sync` — record `lastSyncAt` + a real `lastSyncOutcome`; this slice may perform a no-op/stub import but must NOT fabricate imported data (importedCount 0 is honest).
- Owner-scoped per ADR-002 (single-user MVP).

## Non-Functional Requirements

- **Security**: no tokens are stored this slice, but the application port must stay token-free so slices 2-3 can add encrypted storage in the adapter without changing the domain. No secret/token in any response or log.
- **Isolation** (ADR-004): provider-neutral ports; any provider-specific behavior stays in an adapter. Sync outcomes are user-readable.
- **Performance**: tiny per-user connection set; trivial queries.

## Data Model Notes

- New domain: `IntegrationConnection` aggregate + `IntegrationProvider` enum (e.g. `WITHINGS`, `GOOGLE_FIT`, `APPLE_HEALTH`) + `SyncOutcome` value (result, importedCount, message).
- Persistence: JDBC adapter + Flyway migration, next free version is **V12** (current head V11 after FOR-125). One column per statement (H2/PostgreSQL convention).
- `lastSyncOutcome` is a small embedded value or columns on the connection row.
- No token table this slice (added in slice 2).

## Edge Cases

- `GET` before any connection → 200 with all known providers DISCONNECTED, never 404.
- Connect when already CONNECTED → idempotent (stays CONNECTED) or 409 — pick one, document; frontend must not break either way.
- Disconnect when already DISCONNECTED → idempotent no-op.
- Sync on a DISCONNECTED provider → 409 or a readable "not connected" outcome; document.
- Unknown `provider` path value → 400 `VALIDATION_ERROR`.

## Open Questions

- Which providers seed the status list (all three, or just Withings for MVP)? Confirm against FOR-57's UI.
- Connect-when-connected and sync-when-disconnected semantics (idempotent vs 409) — pick the simplest that keeps the FOR-57/FOR-123 UI clean; document.
- Does this slice's sync do a real stub import (0 rows) or return a "no sync backend yet" outcome? Either is acceptable; document — real import is slice 3.
