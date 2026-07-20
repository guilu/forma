# FOR-159 API Spec

> Adds one endpoint to the existing weekly-tracking controller from FOR-155
> (`delivery/tracking/WeeklyTrackingRecordController`, base `/api/v1/tracking/weekly`). Aligns with
> ADR-005. Confirm exact paths against `ApiPaths.java` and the existing controller before coding.

## Existing endpoints (FOR-155, for context — not changed here)

- `GET  /api/v1/tracking/weekly` — list weekly tracking rows (owner-scoped).
- `GET  /api/v1/tracking/weekly/{week}` — one week.
- `POST /api/v1/tracking/weekly` — upsert a week.

## New endpoint (this story)

### DELETE /api/v1/tracking/weekly/{week}

Delete one weekly tracking row for the owner.

## Request

- Path param `week` — the week identifier used by the existing GET/POST (match its exact type/format,
  e.g. ISO week or week-start date; confirm against FOR-155).
- No request body.

## Response

- `204 No Content` on successful delete (or `200` with an empty body — match the codebase convention).
- No payload.

## Errors

- `404 NOT_FOUND` — no tracking row for that `week` (owner-scoped), via `GlobalExceptionHandler`.
- `400 VALIDATION_ERROR` — malformed `week` path param.

## Authorization

Single-user MVP (ADR-002), owner-scoped — the delete only affects the owner's row; no cross-owner
delete. No account/owner path segment or auth header accepted yet (same posture as FOR-155).

## Validation

- `week` must be a well-formed identifier matching the GET/POST contract; otherwise 400.
- Deleting a missing week → 404 (not a silent success), so the UI can message it.
