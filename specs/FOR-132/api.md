# FOR-132 API Spec

> This slice adds NO new endpoints. It makes the EXISTING FOR-126 sync endpoint perform
> a real import. Confirm the current shape against `delivery/integrations/*`. No token
> or secret ever appears in a response.

## Endpoints

### POST /api/v1/integrations/{provider}/sync (behavior changed)

Now performs a real Withings import (was a stub in FOR-126). Ensures a valid token
(refresh if needed), calls Withings Getmeas, maps + dedups measure groups into
`BodyMeasurement`, and returns a real outcome.

## Request

Unchanged: `POST /api/v1/integrations/withings/sync` — no body.

## Response

Before (FOR-126 stub):
```json
{ "result": "OK", "importedCount": 0, "lastSyncAt": "…", "message": null }
```
After (FOR-132 — real import):
```json
{ "result": "OK", "importedCount": 3, "duplicatesSkipped": 12, "lastSyncAt": "2026-07-16T18:00:00Z", "message": null }
```
Failure outcomes (no secret in the body):
```json
{ "result": "NEEDS_REAUTH", "importedCount": 0, "message": "Reconnect Withings to continue syncing." }
```
```json
{ "result": "ERROR", "importedCount": 0, "message": "Withings temporarily unavailable, try again later." }
```
- `NOT_CONNECTED` outcome (sync on a disconnected provider) unchanged from FOR-126.
- `duplicatesSkipped` is added to the sync outcome (document if the DTO is extended).

## Errors

- Transport/API-level: surfaced as a readable `result` in the 200 outcome body (ERROR/NEEDS_REAUTH), consistent with FOR-126's outcome model — not an HTTP 5xx to the client, unless the existing convention differs (follow the repo).
- Unknown provider → 400 (unchanged from FOR-126).

## Authorization

Single-user MVP (ADR-002), owner-scoped. Tokens never returned; there is no endpoint that reads a token back.

## Validation

- `provider` must be a known enum value.
- No request body.
- Never log or echo tokens, `code`, `state`, or measure payloads containing secrets.
