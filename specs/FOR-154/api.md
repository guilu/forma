# FOR-154 API Spec

> Reuses the existing workout-template / exercise-catalog read models. The payload gains the richer
> per-exercise programming (and the new rep-scheme representation). Confirm paths against
> `ApiPaths.java` (`/api/v1`) and the training controller. Aligns with ADR-005.

## Endpoints

### GET /api/v1/training/templates (existing — confirm path)

The strength templates. After this slice: 3 blocks × 5 exercises with per-exercise sets/reps/RIR/rest.

### GET /api/v1/training/exercises (existing — confirm path)

The exercise catalog, now including the ~5 added exercises with their `primaryMuscles`.

## Response (template item)

```json
{
  "workoutType": "PUSH",
  "items": [
    { "exerciseId": "dumbbell-bench-press", "order": 1, "sets": 4, "repScheme": "RANGE", "repsMin": 8, "repsMax": 12, "rir": 2, "restSeconds": 90 },
    { "exerciseId": "push-up", "order": 3, "sets": 3, "repScheme": "AMRAP", "rir": 1, "restSeconds": 60 },
    { "exerciseId": "plank", "order": 5, "sets": 3, "repScheme": "TIME", "durationSecondsMin": 45, "durationSecondsMax": 75, "rir": 2, "restSeconds": 45 }
  ]
}
```
- `repScheme` + the optional fields reflect the chosen `StrengthWorkoutItem` extension; RANGE keeps `repsMin/repsMax`. Field names to match the final DTO.

## Errors

- No new client input. A template referencing an unknown exercise id fails fast at load (existing behavior), not at request time.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with existing training endpoints.

## Validation

- No request body. Item invariants (sets ≥ 1, range bounds, rir ≥ 0, rest ≥ 0) enforced in the domain; new scheme fields validated there too.
