# FOR-99 API Spec

## Endpoints

### GET /api/v1/training/workouts

Returns all strength workout templates (FOR-25) with per-exercise details
resolved from the FOR-24 exercise catalog. Read-only, in-code data, no
persistence.

### GET /api/v1/training/workouts/{type}

Returns one template by `WorkoutType` (e.g. `PUSH`, `PULL`, `LEGS`). `404` when
the type is unknown.

## Request

No body. `{type}` path variable is a `WorkoutType` name for the by-type endpoint.

## Response

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

- `workoutType`: `PUSH` | `PULL` | `LEGS` (serialized as its name).
- `exerciseName`: resolved from the FOR-24 catalog; falls back to `exerciseId`
  if unresolved (should not happen — catalog integrity is enforced at build).
- `repsMin`/`repsMax`: the rep range; `restSeconds`: rest between sets; `rir`:
  target reps in reserve.

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 404 Not Found — `NOT_FOUND`: no template for the given `{type}`.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

`{type}` must be a valid `WorkoutType`; an unknown value yields 404 (not 500).
