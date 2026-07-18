# FOR-154 Test Plan

Strict TDD: failing tests first (new exercises → item rep-schemes → templates), then implement.

## Scope

The added catalog exercises, the `StrengthWorkoutItem` rep-scheme extension, and the 3 rebuilt
templates with per-exercise programming.

## Domain Tests

- `ExerciseCatalog` includes dumbbell bench press, lateral raise, biceps curl, rear-delt fly, calf raise, each with non-empty `primaryMuscles` and home equipment.
- `StrengthWorkoutItem`: RANGE case keeps existing invariants; AMRAP case (no upper bound) is valid; TIME case (45–75s hold) is valid; invalid combinations rejected.
- Each template has 5 items (order 1–5) with sets/reps/RIR/rest matching the Fuerza table exactly (spot-check every block).
- Fail-fast: a template item with an unknown exercise id throws at load.

## Application / API Tests

- `GET /api/v1/training/templates` returns 3 blocks × 5 exercises with per-exercise programming and rep-scheme.
- `GET /api/v1/training/exercises` includes the new exercises with `primaryMuscles`.
- FOR-136 muscle-map still derives a map from the strength session's exercises (no regression).

## Edge Cases

- AMRAP items (Flexiones, Dominadas) have no `repsMax`.
- Plancha is a TIME hold (45–75s), not reps.
- Zancadas "/pierna" preserved as a note, not overloaded into `repsMax`.

## Fixtures

- The Fuerza table (per exercise: block, sets, reps/scheme, RIR, rest) as expected-value fixtures.
- Existing `WorkoutTemplateCatalog` / `ExerciseCatalog` tests updated from the 3-per-block uniform scheme.
