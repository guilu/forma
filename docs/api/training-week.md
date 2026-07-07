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
        { "kind": "STRENGTH", "title": "Fuerza · Empuje", "detail": "3 ejercicios", "status": "PLANNED" }
      ]
    },
    {
      "dayOfWeek": "SATURDAY",
      "rest": false,
      "sessions": [
        { "kind": "RUNNING", "title": "Tirada larga", "detail": "4.0 km", "status": "PLANNED" }
      ]
    },
    { "dayOfWeek": "SUNDAY", "rest": true, "sessions": [] }
  ]
}
```

- `kind`: `RUNNING` or `STRENGTH`.
- `rest`: `true` when the day has no sessions.
- `status`: `PLANNED` in this version.

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape for
unexpected failures (`INTERNAL_ERROR`, 500). No request input, so no
validation/not-found cases.

## Authorization

None enforced yet — single-user MVP (ADR-002).
