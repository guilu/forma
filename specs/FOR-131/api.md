# FOR-131 API Spec

> Security-sensitive. **No endpoint ever returns a token, refresh token, `code`, or
> `state`.** Extends the FOR-126 integrations endpoints. Confirm exact paths against
> `ApiPaths.java` + the existing `IntegrationController`.

## Endpoints

### POST /api/v1/integrations/{provider}/connect (changed)

Starts OAuth: returns the provider authorization URL and persists a short-lived state/PKCE challenge. No longer flips status directly (that happens at the callback).

### GET /api/v1/integrations/{provider}/callback?code=...&state=... (new)

OAuth redirect target: validates `state`, exchanges `code` for tokens, stores them encrypted, marks CONNECTED. Server-to-server redirect landing; the SPA does not call this via fetch.

### DELETE /api/v1/integrations/{provider} (changed)

Disconnect now also revokes/forgets the stored tokens.

## Request

`POST /api/v1/integrations/withings/connect` — no body.
`GET /api/v1/integrations/withings/callback?code=...&state=...` — provider-supplied query params.

## Response

`POST /api/v1/integrations/withings/connect`
```json
{ "authorizationUrl": "https://account.withings.com/oauth2_user/authorize2?client_id=...&state=...&redirect_uri=...&scope=...&code_challenge=..." }
```
`GET /api/v1/integrations/withings/callback` — on success, either:
- a redirect (302) back to the SPA integrations page, OR
- `200` with the updated connection status:
```json
{ "provider": "WITHINGS", "status": "CONNECTED", "connectedAt": "2026-07-16T15:00:00Z" }
```
Document the chosen contract (redirect vs JSON) — it affects the frontend story.

## Errors

- 400 Bad Request — unknown provider; callback with missing/mismatched/expired/replayed `state`; missing `code`.
- 409 Conflict — (if chosen) connect when already connected.
- 502/503 — Withings token exchange/refresh failure; readable outcome, **no secret in the body**.
- Status GET (FOR-126) unchanged — never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. **Tokens are never returned**; there is no endpoint that reads a token back.

## Validation

- `provider` must be a known enum value → else 400.
- Callback `state` must match a stored, unexpired, single-use challenge (CSRF/PKCE) → else 400.
- Callback `code` required.
- Never log or echo `code`, `state`, or any token.
- OAuth client credentials, redirect URI and the encryption key come from config/env, never request input, never committed.
