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

## `GET /api/v1/training/weekly-summary`

Returns the current week's training adherence summary (FOR-28/FOR-98): planned
vs. completed running and strength sessions, and planned vs. completed running
distance. Computed on demand from the FOR-26 schedule and FOR-27 completion
status; no persistence.

`200 OK`

```json
{
  "plannedRunningSessions": 3,
  "completedRunningSessions": 2,
  "plannedStrengthSessions": 3,
  "completedStrengthSessions": 1,
  "totalPlannedRunningKm": 8.6,
  "completedRunningKm": 5.0,
  "message": "Carrera: 2/3 sesiones (5.0/8.6 km). Fuerza: 1/3 sesiones."
}
```

Empty week (nothing planned):

```json
{
  "plannedRunningSessions": 0,
  "completedRunningSessions": 0,
  "plannedStrengthSessions": 0,
  "completedStrengthSessions": 0,
  "totalPlannedRunningKm": 0.0,
  "completedRunningKm": 0.0,
  "message": "No hay entrenamientos planificados esta semana."
}
```

- Counts are integers; km are one-decimal doubles (as FOR-28 rounds them).
- Completed running km includes only completed sessions.
- No request body, no parameters (current week only).

## `GET /api/v1/training/workouts`

Returns all strength workout templates (FOR-25) with per-exercise details
resolved from the FOR-24 exercise catalog. Read-only, in-code data, no
persistence.

## `GET /api/v1/training/workouts/{type}`

Returns one strength workout template by `WorkoutType` (`PUSH`, `PULL`,
`LEGS`). `404` when the type is unknown.

`200 OK` (list):

```json
[
  {
    "workoutType": "PUSH",
    "items": [
      {
        "exerciseId": "push-up",
        "exerciseName": "Flexiones",
        "order": 1,
        "sets": 3,
        "repsMin": 8,
        "repsMax": 12,
        "restSeconds": 90,
        "rir": 2
      }
    ]
  }
]
```

- `exerciseName`: resolved from the FOR-24 catalog; falls back to
  `exerciseId` if unresolved (should not happen — catalog integrity is
  enforced at build).
- `repsMin`/`repsMax`: the rep range; `restSeconds`: rest between sets;
  `rir`: target reps in reserve.

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
