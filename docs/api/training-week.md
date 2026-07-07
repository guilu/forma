# Training week API

Read-only endpoint backing the weekly training calendar (FOR-26). Follows the
shared [API conventions](../api-conventions.md) (ADR-005). Lives under `/api/v1`.

## `GET /api/v1/training/week`

Returns the current week's composed training calendar (Monday through Sunday),
combining planned running sessions (FOR-22/FOR-23) and strength templates
(FOR-25). Days without a session are rest days.

The composition uses a simple MVP scheduling policy (no dates, no week
navigation): running from plan week 1 on its days (Tue/Thu/Sat), strength on
Monday (push), Wednesday (pull) and Friday (legs & core); Sunday is rest. All
sessions are `PLANNED` (completion is FOR-27).

`200 OK`

```json
{
  "days": [
    {
      "dayOfWeek": "MONDAY",
      "rest": false,
      "sessions": [
        { "id": "MONDAY:STRENGTH", "kind": "STRENGTH", "title": "Fuerza · Empuje", "detail": "3 ejercicios", "status": "PLANNED" }
      ]
    },
    {
      "dayOfWeek": "SATURDAY",
      "rest": false,
      "sessions": [
        { "id": "SATURDAY:RUNNING", "kind": "RUNNING", "title": "Tirada larga", "detail": "4.0 km", "status": "COMPLETED", "notes": "Buenas sensaciones" }
      ]
    },
    { "dayOfWeek": "SUNDAY", "rest": true, "sessions": [] }
  ]
}
```

- `id`: stable session id (`<DAY>:<KIND>`), used to mark completion.
- `kind`: `RUNNING` or `STRENGTH`.
- `rest`: `true` when the day has no sessions.
- `status`: `PLANNED`, `COMPLETED` or `SKIPPED` (FOR-27).
- `notes`: optional completion note; omitted when absent.

## `PATCH /api/v1/training/sessions/{id}/status`

Marks a session's completion status (FOR-27). Works for running and strength
sessions.

Request:

```json
{ "status": "COMPLETED", "notes": "Buenas sensaciones" }
```

`status` is required, one of `PLANNED` | `COMPLETED` | `SKIPPED`; `notes` is
optional. Any status can be set (including reverting) in this version.

`200 OK`:

```json
{ "id": "SATURDAY:RUNNING", "status": "COMPLETED", "notes": "Buenas sensaciones" }
```

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 400 Bad Request — `VALIDATION_ERROR`: missing/invalid `status`.
- 404 Not Found — `NOT_FOUND`: no session with the given id in the current week.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).
