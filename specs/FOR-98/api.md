# FOR-98 API Spec

## Endpoints

### GET /api/v1/training/weekly-summary

Returns the current week's training adherence summary (FOR-28): planned vs
completed running and strength sessions, and planned vs completed running
distance. Computed on demand; no persistence.

## Request

No body, no parameters (current week only).

## Response

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

Empty week:

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

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 500 — `INTERNAL_ERROR`: unexpected failures only. An empty week is not an error.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

No input to validate (read-only, no parameters).
