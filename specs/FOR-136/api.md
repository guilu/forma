# FOR-136 API Spec

> Muscle-map subset of `specs/FOR-104/api.md`, scoped to this slice. Aligns with ADR-005.
> Confirm the exact path + session-id scheme against `ApiPaths.java` and the training delivery package.

## Endpoints

### GET /api/v1/training/sessions/{sessionId}/muscle-map

Worked-muscle map for a strength session, derived from its exercises' `primaryMuscles`.

## Request

`GET /api/v1/training/sessions/MONDAY:STRENGTH/muscle-map` — `sessionId` is the stable
strength session id (weekly-schedule `<DAY>:STRENGTH`, or the workout-template id — document the chosen scheme).

## Response

```json
{
  "sessionId": "MONDAY:STRENGTH",
  "muscles": [
    { "muscle": "PECHO", "load": "HIGH" },
    { "muscle": "TRICEPS", "load": "MEDIUM" },
    { "muscle": "HOMBRO", "load": "MEDIUM" }
  ]
}
```
- Non-strength (running/rest) session → `{ "sessionId": "...", "muscles": [] }` (empty, 200).
- `muscles` derived from the session's exercises' `primaryMuscles`; `load` from exercise-frequency thresholds (documented).

## Errors

- 404 Not Found — unknown session id (not resolvable to a schedule day / template).
- No client input beyond the path segment.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with the existing training endpoints.

## Validation

- `sessionId` must resolve to a known session/template → else 404.
- No request body.
