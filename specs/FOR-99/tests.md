# FOR-99 Test Plan

## Scope

Verify the workout-template endpoints return templates with per-exercise details
(name + sets/reps/rest/RIR) resolved from the catalog, and 404 on unknown type.

## Domain Tests

N/A — templates/catalog covered by FOR-24/FOR-25.

## Application Tests

N/A — reuses `WorkoutTemplateService`.

## API Tests

- `GET /api/v1/training/workouts` returns all templates, each with `workoutType`
  and ordered items carrying `exerciseName`, `sets`, `repsMin`, `repsMax`,
  `restSeconds`, `rir`.
- `GET /api/v1/training/workouts/{type}` returns a single template.
- Unknown `{type}` → 404 `NOT_FOUND` (standard error shape).
- Exercise names are resolved from the FOR-24 catalog (not raw ids).

## UI Tests

N/A — backend story.

## Edge Cases

- Unknown workout type → 404.
- Defensive fallback if an item's exercise id is not in the catalog.

## Fixtures

- The in-code `WorkoutTemplateCatalog` (Push/Pull/Legs) + `ExerciseCatalog`.
