# FOR-131 Spec

> ⚠️ **Security-sensitive.** OAuth + encrypted token storage. Requires a security
> review before merge. Real end-to-end against Withings needs real credentials and is
> out of automated-test scope.

Jira: https://dbhlab.atlassian.net/browse/FOR-131
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-103 [STUB] Integrations backend (slice 2 of 3+). Builds on FOR-126.

## Summary

Real Withings OAuth connect/disconnect on top of the FOR-126 connection shell:
authorization URL, callback + code→token exchange, encrypted token storage, token
refresh, and disconnect that revokes/forgets tokens. NO real Withings data sync into
`BodyMeasurement` this slice — sync stays the FOR-126 stub (`importedCount: 0`); that is
FOR-103 slice 3. See `specs/FOR-103/` for full scope + security requirements.

## Registered Withings application (real, provided by product owner)

A Withings Public Cloud app exists (Production, name "forma"):

- **Registered OAuth2 redirect URL**: `https://forma.diegobarrioh.dev/auth` — this is a
  **frontend/SPA route**, NOT a backend API path. Withings redirects the browser there
  with `code` + `state`; the SPA then relays them to the backend to complete the
  exchange (see Flow). This resolves the spec's callback-contract open question.
- **Scope**: `user.metrics` — grants access to body-composition measures (weight, fat,
  muscle, water) via Withings *Measure — Getmeas*, the data slice 3 maps into
  `BodyMeasurement`.
- **Client id + client secret**: issued by Withings, held only by the product owner.
  They are **secrets** — read from config/env (`WITHINGS_CLIENT_ID`,
  `WITHINGS_CLIENT_SECRET`), never committed, never placed in specs/code/tests. The
  token-encryption key is likewise env-provided (`WITHINGS_TOKEN_ENC_KEY` or equivalent).
  Tests use placeholders + recorded fixtures.

## Repository baseline (FOR-126, merged)

- `integration_connection` table keyed by `(owner_id, provider)`, one row per provider, **token-free by design** (V12 migration documents this).
- `IntegrationService.connect/sync/disconnect/status`; `connect` currently just flips status (mock). `IntegrationController`: `GET /integrations`, `POST /{provider}/connect`, `POST /{provider}/sync`, `DELETE /{provider}` — **no callback endpoint yet**.
- Application port is **token-free** — this slice keeps it that way; tokens live only in the adapter/persistence layer.

## User/System Flow

1. User taps "Conectar Withings" → `POST /api/v1/integrations/withings/connect` returns a Withings **authorization URL** (built with `redirect_uri=https://forma.diegobarrioh.dev/auth`, scope `user.metrics`, `state`, PKCE). Backend persists a short-lived single-use state/PKCE challenge. Status stays DISCONNECTED (or PENDING) until completion.
2. User authorizes at Withings → Withings redirects the browser to the registered SPA route `https://forma.diegobarrioh.dev/auth?code=...&state=...`.
3. The SPA reads `code` + `state` from the URL and relays them to the backend: `POST /api/v1/integrations/withings/callback` with `{ code, state }`.
4. Backend validates `state`, exchanges `code` for tokens, **encrypts + stores** them, marks the connection CONNECTED.
5. On later use, if the access token is expired, refresh it using the stored refresh token.
6. `DELETE /api/v1/integrations/withings` → revoke/forget tokens; nothing remains at rest.

## Functional Requirements

- Build the Withings authorization URL from config: `client_id` (`WITHINGS_CLIENT_ID`), `redirect_uri=https://forma.diegobarrioh.dev/auth`, `scope=user.metrics`, `state`, PKCE `code_challenge`.
- Callback endpoint is a **backend `POST` the SPA calls with `{ code, state }`** (the browser redirect lands on the SPA `/auth` route, not the backend). Validate `state` against a stored, unexpired, single-use challenge (CSRF/PKCE); exchange `code` for access+refresh tokens using `WITHINGS_CLIENT_SECRET`; store encrypted; mark CONNECTED with `connectedAt`.
- Encrypted token storage in a NEW table (migration **V15**), separate from `integration_connection`; access token, refresh token, expiry — all encrypted at rest.
- Token refresh when the access token is expired (or documented if the provider refresh path can't be exercised without live credentials).
- Disconnect revokes/forgets the stored tokens.
- Withings-specific logic (URL build, token exchange, refresh, revoke) lives in an **adapter** behind provider-neutral application ports (ADR-004).

## Non-Functional Requirements

- **Security (primary)**: tokens encrypted at rest; OAuth state/PKCE to prevent CSRF; secrets (client id/secret, redirect URI, encryption key) from config/env, never committed; **never log tokens, `code`, or `state`** (ADR-004, ADR-008). No secret in any response.
- **Isolation**: provider payloads/token shapes stay inside the adapter (ADR-004).
- **Owner-scoped** per ADR-002.
- The application port stays token-free — no token accessor (guards slices' boundary).

## Data Model Notes

- New table (V15): encrypted provider tokens keyed by `(owner_id, provider)` — encrypted access/refresh token columns + expiry. Do NOT add tokens to `integration_connection`.
- OAuth state challenge: a short-lived persisted table OR a documented in-memory store; must expire and be single-use.
- Reuse FOR-126 `IntegrationConnection` for status transitions (add a PENDING/AWAITING_CALLBACK state if needed; document).
- No new dependency leaking Withings types into the domain.

## Edge Cases

- Callback with mismatched/expired/replayed `state` → reject, no connection created, no tokens stored.
- Token exchange failure (Withings error) → connection not marked CONNECTED; readable outcome, no secret leak.
- Refresh failure → mark connection needing re-auth; do not silently drop.
- Disconnect while tokens exist → tokens removed; disconnect when already disconnected → idempotent.
- Connect when already CONNECTED → document (re-auth vs 409).

## Open Questions

- Encryption approach for tokens at rest: app-level envelope encryption (key from env) vs DB-native — decide + document, consistent with ADR-003. Key management (env var) documented.
- OAuth state store: persisted (small table, needs migration) vs in-memory (simpler, lost on restart) — pick for MVP + document.
- ~~Callback response: redirect vs JSON~~ **RESOLVED**: the registered redirect URI is the SPA route `https://forma.diegobarrioh.dev/auth`; Withings redirects the browser there, and the SPA relays `code`+`state` to the backend via `POST /api/v1/integrations/withings/callback` returning the updated connection status as JSON. A future frontend story implements the `/auth` route.
- Whether Withings token refresh/revoke can be exercised in tests without live credentials — use recorded fixtures; document what is and isn't covered.
