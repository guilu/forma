# FOR-103 Spec

> ⚠️ **Epic-sized, security-sensitive stub.** OAuth + encrypted token storage. This
> folder captures full scope and proposes slicing; it does NOT create Jira issues.
> Implement one slice per PR with `jira-sdd-ai`. Every slice touching tokens/OAuth
> requires a security review before merge.

Jira: https://dbhlab.atlassian.net/browse/FOR-103
Epic: FOR-96 UI Backend Enablers — Foundations

## Summary

Build the real external-integrations backend: connect/disconnect providers (starting
with Withings), store tokens securely, sync provider data into FORMA's normalized
`BodyMeasurement` domain, and expose connection + last-sync + manual-sync status.
Today there is **no integrations backend at all** — FOR-57 UI runs against a mock,
the FOR-51 sync widget and sidebar "Withings · Conectado" are static/hardcoded, and
the frontend `connect/sync/disconnect` API calls are typed `Promise<never>`.

## User/System Flow

1. User opens Integraciones (FOR-57) → GET connection status per provider.
2. User taps "Conectar Withings" → backend starts OAuth authorization, redirects to Withings, handles the callback, exchanges the code for tokens, stores them **encrypted**.
3. A sync job pulls Withings measures and **normalizes** them into `BodyMeasurement` (`MeasurementSource = WITHINGS`), idempotently, with duplicate detection.
4. User (or a schedule) triggers a manual sync → backend runs the sync, updates last-sync + status.
5. User taps "Desconectar" → backend revokes/forgets tokens and marks the provider disconnected.

## Functional Requirements

- **OAuth connect/disconnect** for Withings: authorization URL, callback handling, token exchange, refresh, disconnect (token revocation/forgetting).
- **Encrypted token storage** — tokens are never exposed to domain services, read models, logs, or the frontend (ADR-004, AGENTS forbidden shortcuts).
- **Sync** Withings measures → `BodyMeasurement` at the adapter boundary; provider payloads never become the primary domain model (ADR-004).
- **Idempotent sync** with mandatory duplicate detection; respect provider rate limits.
- **Status endpoints**: per-provider connection state, last-sync timestamp, last-sync outcome (user-readable, no secret leakage), manual-sync trigger.
- Later providers (Google Fit, Apple Health) reuse the same provider-neutral ports.

## Non-Functional Requirements

- **Security** (primary): encrypted tokens at rest; no token/secret in logs or errors; OAuth state/PKCE to prevent CSRF; secrets from config/env, never committed.
- **Reliability**: sync failures are observable and user-readable without leaking secrets; retries respect rate limits.
- **Privacy**: imported health data is owner-scoped (ADR-002 single-user MVP); do not log measure values at INFO.
- **Isolation**: provider-specific failure handling stays inside the adapter.

## Data Model Notes

- New domain/application: provider-neutral `IntegrationConnection` (provider, status, connectedAt, lastSyncAt, lastSyncOutcome) behind an application port; **no tokens in the domain**.
- New adapter: `WithingsClient`/adapter under `adapter/` mapping Withings payloads → normalized measures; token store is an adapter concern.
- Sync target is the existing `BodyMeasurement` aggregate + `MeasurementSource` enum (already present) — extend the enum with `WITHINGS` if absent.
- New migration(s): **V11+** (head is V10). Token storage column(s) encrypted; connection/status tables provider-neutral.
- Duplicate detection keyed on (provider, external measure id or measured-at + type).

## Edge Cases

- OAuth callback with mismatched/expired state → reject, no connection created.
- Token refresh failure → mark connection needing re-auth; never silently drop data.
- Sync returning already-imported measures → no duplicates created (idempotent).
- Disconnect while a sync is in flight → safe; no orphaned tokens left at rest.
- Provider rate-limit / 429 → back off, surface a user-readable status, do not hammer.
- Withings returns a measure type FORMA doesn't model → skip + document, never force it into the domain.

## Proposed story slices

1. **Provider-neutral connection domain + status read model + API** (mock/no real OAuth yet) — replaces the frontend mock with a real status contract.
2. **Withings OAuth connect/disconnect + encrypted token storage** (security review required).
3. **Withings sync → BodyMeasurement** (idempotent, duplicate detection, rate limits).
4. **Manual sync + last-sync status/outcome endpoints.**
5. **(Later) Google Fit / Apple Health adapters** behind the same ports.

## Open Questions

- Does `MeasurementSource` already include a provider value (e.g. `WITHINGS`), or must the enum be extended? Verify before slice 3.
- Encryption approach for tokens at rest (app-level envelope vs DB-native) — decide in slice 2's design, consistent with ADR-003.
- Scheduling: is automatic periodic sync in scope, or manual-only for MVP?
- Where do OAuth client credentials come from in local/dev vs CI (config/secret management)?
