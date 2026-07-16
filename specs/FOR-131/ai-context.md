# FOR-131 AI Context

## Story

FOR-131 — Withings OAuth connect/disconnect and encrypted token storage. Slice 2 (of 3+) of FOR-103 [STUB] Integrations backend. **Security-sensitive — requires a security review before merge.**

## Intent

Turn FOR-126's mock connect into a real Withings OAuth flow: authorization URL, callback + code→token exchange, encrypted token storage, refresh, and token-revoking disconnect. This is the prerequisite for real sync (slice 3). Success = a provider can be genuinely connected with tokens stored encrypted, and disconnect forgets them. Real Withings measures sync stays the FOR-126 stub.

## Relevant Documents

- `specs/FOR-103/` — full integrations scope + security requirements.
- `specs/FOR-126/` — the connection shell this builds on (token-free port + table by design).
- `AGENTS.md` — forbidden shortcuts: never commit/log tokens; never expose tokens to domain/frontend.
- `docs/adr/ADR-004-integrations.md` — provider-neutral ports, adapters at the boundary, no secret leakage.
- `docs/adr/ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`, `ADR-008-observability.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-131

## Domain / Repo Notes

- FOR-126 (merged) provides: `IntegrationConnection` domain, `IntegrationService` (connect mock/sync stub/disconnect/status), `JdbcIntegrationRepository`, `integration_connection` table (token-free), `delivery/integrations/*` (connect/sync/disconnect/list; NO callback).
- The application port is token-free — KEEP it that way. Encrypted token storage lives entirely in the adapter/persistence layer, in a NEW table (migration **V15**, head is V14).
- Withings-specific OAuth logic (authorize URL, token exchange, refresh, revoke) goes in a new adapter behind provider-neutral ports (ADR-004) — do not leak Withings shapes into the domain.
- Add a connection status like PENDING/AWAITING_CALLBACK if needed between connect and callback; document.

## Architectural Constraints

- Hexagonal: application ports stay token-free; Withings + encryption are adapter concerns.
- OAuth state/PKCE for CSRF; single-use, expiring state challenge (persisted table or documented in-memory).
- Secrets from config/env only — never committed. Real registered Withings app (Production, "forma"): `redirect_uri=https://forma.diegobarrioh.dev/auth` (a SPA route, NOT the backend), scope `user.metrics`. Env: `WITHINGS_CLIENT_ID`, `WITHINGS_CLIENT_SECRET`, `WITHINGS_TOKEN_ENC_KEY` (names indicative). Callback is a backend `POST /{provider}/callback` the SPA calls with `{code,state}` after the browser lands on `/auth`.
- Never log tokens, `code`, or `state` (ADR-004/ADR-008).
- Owner-scoped per ADR-002.
- New migration is **V15** (current head V14); one column per statement.

## Common Pitfalls

- Adding token columns to `integration_connection` — use a separate encrypted store; keep that table + the port token-free.
- Leaking a token/`code`/`state` into a response, header, log, or the domain — assert against this.
- Committing OAuth client credentials or a real encryption key.
- Calling the live Withings API in tests — use recorded fixtures.
- Skipping OAuth state validation (CSRF hole).
- Building real measures sync here — that is slice 3; sync stays the FOR-126 stub.

## Suggested Implementation Order

1. Encrypted token store: adapter + `V15` migration + round-trip test proving encryption at rest; application port stays token-free.
2. OAuth state/PKCE challenge store (persisted or in-memory) with single-use + expiry (+ tests).
3. Withings adapter: authorize URL builder + token exchange + refresh + revoke, against recorded fixtures.
4. Wire into `IntegrationService`/controller: `connect` returns authorization URL; new `callback` validates state, exchanges, stores encrypted, marks CONNECTED; `disconnect` revokes. API tests + token-leak assertions.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: connect returns an authorization URL; callback validates state and stores encrypted tokens (stored bytes ≠ plaintext); mismatched/expired state rejected; disconnect leaves no tokens at rest; no token/`code`/`state` in any response or log; secrets from config; no new token column on `integration_connection`. Live Withings E2E is out of automated scope (needs real credentials) — document. Flag for security review before merge.
