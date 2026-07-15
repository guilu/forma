# FOR-126 API Spec

> Connection/status/command subset of `specs/FOR-103/api.md`, scoped to this slice.
> No OAuth authorization URL, no callback, no token — those are FOR-103 slice 2.
> **No endpoint ever returns a token or secret.** Confirm exact paths against
> `ApiPaths.java` at implementation time.

## Endpoints

### GET /api/v1/integrations

Per-provider connection status, last-sync time and last-sync outcome.

### POST /api/v1/integrations/{provider}/connect

Mark the provider connected (mock, no OAuth this slice). Resolves.

### POST /api/v1/integrations/{provider}/sync

Trigger a manual sync now; records and returns a real `lastSyncOutcome`.

### DELETE /api/v1/integrations/{provider}

Disconnect; mark disconnected. Resolves.

## Request

`POST /api/v1/integrations/withings/connect` — no body.
`POST /api/v1/integrations/withings/sync` — no body.
`DELETE /api/v1/integrations/withings` — no body.

## Response

`GET /api/v1/integrations`
```json
{
  "providers": [
    {
      "provider": "WITHINGS",
      "status": "CONNECTED",
      "connectedAt": "2026-07-15T08:00:00Z",
      "lastSyncAt": "2026-07-15T09:00:00Z",
      "lastSyncOutcome": { "result": "OK", "importedCount": 0, "message": null }
    },
    { "provider": "GOOGLE_FIT", "status": "DISCONNECTED", "connectedAt": null, "lastSyncAt": null, "lastSyncOutcome": null }
  ]
}
```
`POST /api/v1/integrations/withings/connect`
```json
{ "provider": "WITHINGS", "status": "CONNECTED", "connectedAt": "2026-07-15T08:00:00Z" }
```
`POST /api/v1/integrations/withings/sync`
```json
{ "result": "OK", "importedCount": 0, "lastSyncAt": "2026-07-15T09:00:00Z", "message": null }
```

## Errors

- 400 Bad Request — unknown `provider` path value.
- 409 Conflict — (if chosen) connect-when-connected or sync-when-disconnected; document the chosen semantics.
- Status GET before any connection → 200 with all providers DISCONNECTED, never 404.
- No 401/OAuth errors this slice (no real auth flow yet).

## Authorization

Single-user MVP (ADR-002), owner-scoped. No token is ever returned; there is no endpoint that reads a token back (no tokens exist this slice).

## Validation

- `provider` must be a known enum value → else 400 `VALIDATION_ERROR`.
- No request bodies to validate this slice (all commands are path-only).
- Never log a token/secret (none exist yet, but keep responses/logs clean for slices 2-3).
