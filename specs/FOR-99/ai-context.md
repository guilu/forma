# FOR-99 AI Context

## Story

FOR-99 — Expose workout templates (exercises) over HTTP
(https://dbhlab.atlassian.net/browse/FOR-99)

## Intent

Make the FOR-25 strength templates reachable over HTTP with per-exercise details
resolved from the FOR-24 catalog, so FOR-53's exercise table stops being a
placeholder. Success is a thin read endpoint returning name + series/reps/rest/
RIR per exercise.

## Relevant Documents

- `AGENTS.md`
- `docs/api/training-week.md`, `docs/api-conventions.md`
- `docs/adr/ADR-005-api-design.md`, `docs/adr/ADR-001-architecture.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-24/` (exercise catalog), `specs/FOR-25/` (workout templates)
- Jira: https://dbhlab.atlassian.net/browse/FOR-99

## Domain Notes

- `WorkoutTemplateService.allTemplates()` / `findByType(WorkoutType)` already
  expose the templates (FOR-25) — reuse them.
- `ExerciseCatalog.findById(id)` (FOR-24) resolves an item's exercise to get its
  `name` / `primaryMuscles`. Items reference exercises by id, never embed them.
- `StrengthWorkoutItem` fields: `exerciseId`, `order`, `sets`, `repsMin`,
  `repsMax`, `restSeconds`, `rir`.
- No workout endpoint exists yet — this is the first controller over FOR-24/25.

## Architectural Constraints

- Thin controller under `delivery/training` on `ApiPaths.V1` (ADR-001). DTO in
  `delivery/training`, distinct from domain (ADR-005). No persistence, no
  progression rules.

## Common Pitfalls

- Returning domain records (`StrengthWorkoutTemplate`) directly.
- Emitting `exerciseId` only (no resolved name) — the UI needs the name.
- Forgetting the 404 path for an unknown workout type.

## Suggested Implementation Order

1. `WorkoutResponse` DTO (workoutType + items with resolved exercise name).
2. Controller `GET /training/workouts` and `GET /training/workouts/{type}`,
   resolving exercises via `ExerciseCatalog`.
3. `@WebMvcTest` (list, by-type, unknown-type 404).

## Validation

Run `./gradlew test spotlessApply` from `backend/`. Confirm the endpoint returns
resolved exercise names + sets/reps/rest/RIR.
