# FOR-103 API Spec

> Proposed shapes aligned with ADR-005. **No endpoint ever returns a token or secret.**
> Confirm exact paths against `ApiPaths.java` at implementation time.

## Endpoints

### GET /api/v1/integrations

List providers with connection status, last-sync time and last-sync outcome.

### POST /api/v1/integrations/{provider}/connect

Start the OAuth authorization flow; returns the provider authorization URL to redirect to.

### GET /api/v1/integrations/{provider}/callback

OAuth redirect callback: validates state, exchanges code for tokens, stores them encrypted. Server-to-server; not called by the SPA directly.

### POST /api/v1/integrations/{provider}/sync

Trigger a manual sync now.

### DELETE /api/v1/integrations/{provider}

Disconnect: revoke/forget tokens, mark disconnected.

## Request

`POST /api/v1/integrations/withings/connect` — no body (or optional return-to hint).

`GET /api/v1/integrations/withings/callback?code=...&state=...` — provider-supplied query params.

## Response

`GET /api/v1/integrations`
```json
{
  "providers": [
    {
      "provider": "WITHINGS",
      "status": "CONNECTED",
      "connectedAt": "2026-07-10T08:00:00Z",
      "lastSyncAt": "2026-07-15T06:00:00Z",
      "lastSyncOutcome": { "result": "OK", "importedCount": 3, "message": null }
    },
    { "provider": "GOOGLE_FIT", "status": "DISCONNECTED", "connectedAt": null, "lastSyncAt": null, "lastSyncOutcome": null }
  ]
}
```
`POST /api/v1/integrations/withings/connect`
```json
{ "authorizationUrl": "https://account.withings.com/oauth2_user/authorize2?..." }
```
`POST /api/v1/integrations/withings/sync`
```json
{ "result": "OK", "importedCount": 3, "duplicatesSkipped": 12, "lastSyncAt": "2026-07-15T06:00:00Z" }
```

## Errors

- 400 Bad Request — unknown provider, invalid/expired OAuth `state`, malformed callback.
- 409 Conflict — connect when already connected, or callback for a connection not awaiting auth.
- 429 Too Many Requests — provider rate limit reached; surfaced as a user-readable status, retried with backoff server-side.
- 502/503 — provider unreachable; sync outcome recorded as a readable failure, **no secret in the body**.
- Status GET before any connection → 200 with all providers `DISCONNECTED`, never 404.

## Authorization

Single-user MVP (ADR-002), owner-scoped. Do not bypass the owner boundary. Tokens are never returned; there is no endpoint that reads a token back.

## Validation

- `provider` must be a known enum value → else 400.
- OAuth `state` must match a stored, unexpired challenge (CSRF/PKCE) → else 400.
- Callback `code` required.
- Reject and never persist unexpected token-bearing fields into any user-facing model.
- Never log `code`, `state`, tokens, or measure values.
