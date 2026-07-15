# FOR-126 AI Context

## Story

FOR-126 — Integrations connection domain, status read model and connect/disconnect/sync API. First implementable slice (1 of 3+) of FOR-103 [STUB] Integrations backend. Blocks FOR-123 (integrations success feedback).

## Intent

Give the integrations experience a real, provider-neutral backend contract: connection status + connect/disconnect/manual-sync commands that actually resolve, replacing FOR-57's mock and the frontend's `Promise<never>` calls. Success = FOR-57 reflects real state and FOR-123 can show real success/error feedback because these calls can now succeed. Real OAuth, encrypted tokens, and real Withings sync are deferred to FOR-103 slices 2-3.

## Relevant Documents

- `specs/FOR-103/` — full integrations scope, security requirements, slicing, open questions (this story is slice 1).
- `AGENTS.md` — hexagonal boundaries, owner-scoping, never log/commit tokens.
- `docs/adr/ADR-004-integrations.md` — provider-neutral ports; adapters at the boundary; sync outcomes observable without leaking secrets.
- `docs/adr/ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-126

## Domain Notes

- **No integrations code exists today** — this slice introduces the connection shell.
- Reuse nothing provider-specific yet; keep the domain provider-neutral (`IntegrationProvider` enum).
- `MeasurementSource` (existing) is the future sync target for slice 3 — not used here.
- Follow the merged enablers exactly for structure: FOR-107 (`V8__user_profile.sql`, controller/DTOs/adapter/service), FOR-110, FOR-125 (`V11__goal.sql`). Migration head is **V11** → new migration is **V12**.

## Architectural Constraints

- Domain framework-free; application port + service; thin controller (new `delivery/integrations` package, mirror `delivery/profile`/`delivery/goals`); JDBC adapter under `adapter/persistence`.
- **The application port must be token-free** so slices 2-3 add encrypted token storage inside the adapter without touching the domain.
- Owner-scoped per ADR-002; never bypass the boundary.
- New migration is **V12** (current head V11); one column per statement (H2/PostgreSQL convention — see V6/V7/V9).
- No OAuth, no token table, no real sync in this slice — do not build ahead.

## Common Pitfalls

- Building OAuth / token storage / real Withings sync here — those are separate slices; keep this one to the connection/status/command shell.
- Fabricating imported measures/counts on sync — `importedCount: 0` is the honest value this slice.
- Returning 404 for an empty status list instead of all-DISCONNECTED.
- Putting a token accessor on the application port (breaks the boundary the later slices rely on).
- Coercing an unknown `provider` instead of 400.

## Suggested Implementation Order

1. `IntegrationConnection` domain + `IntegrationProvider` enum + `SyncOutcome` (+ tests): state transitions, no tokens.
2. Application port + service (status read model, connect/disconnect/sync), owner-scoped.
3. JDBC adapter + `V12` migration (+ persistence round-trip test).
4. `delivery/integrations` controller + DTOs (+ API tests) per `api.md`.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: status list defaults all-DISCONNECTED (200, not 404); connect → CONNECTED; disconnect → DISCONNECTED; sync returns a real outcome with importedCount 0 (no fabrication); unknown provider → 400; no token accessor on the port. Then FOR-123 (frontend) can wire success/error feedback because connect/sync/disconnect now resolve.
