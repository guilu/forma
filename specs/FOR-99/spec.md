# FOR-99: Expose workout templates (exercises) over HTTP

Jira: https://dbhlab.atlassian.net/browse/FOR-99
Epic: FOR-95 UI Backend Enablers

## Summary

Expose the FOR-25 strength workout templates — with per-exercise sets/reps/rest/
RIR resolved against the FOR-24 exercise catalog — over HTTP, so the training UI
(FOR-53) can render a real exercise list instead of a placeholder. Thin
controller over the existing `WorkoutTemplateService` + `ExerciseCatalog`; no new
domain.

## User/System Flow

1. Client calls `GET /api/v1/training/workouts` (all templates) or
   `GET /api/v1/training/workouts/{type}` (one type).
2. The controller reads templates via `WorkoutTemplateService`, resolves each
   item's exercise via `ExerciseCatalog.findById`, and maps to a response DTO.
3. FOR-53 renders the per-exercise table (name, series, reps, rest, RIR).

## Functional Requirements

- Add read endpoint(s) under `ApiPaths.V1 + "/training/workouts"` in a thin
  controller, delegating to `WorkoutTemplateService.allTemplates()` /
  `findByType(WorkoutType)` (FOR-25).
- For each `StrengthWorkoutItem`, resolve the referenced exercise from
  `ExerciseCatalog` (FOR-24) so the response carries the human-readable exercise
  **name** (and optionally primary muscles), not just the id.
- Response per template: `workoutType` + ordered items with `exerciseId`,
  `exerciseName`, `order`, `sets`, `repsMin`, `repsMax`, `restSeconds`, `rir`.
- Response DTO distinct from the domain types (ADR-005); controller thin
  (ADR-001); no progression/scheduling rules exposed.

## Non-Functional Requirements

- Deterministic (in-code catalog); no persistence.
- Unknown/dangling exercise id must not crash — the catalog guarantees integrity
  at build time, but the mapper should degrade gracefully (fall back to the id).

## Data Model Notes

Reuses `application/WorkoutTemplateService`, `domain/StrengthWorkoutTemplate`
(`workoutType`, `items`), `domain/StrengthWorkoutItem` (`exerciseId`, `order`,
`sets`, `repsMin`, `repsMax`, `restSeconds`, `rir`), `domain/WorkoutType`, and
`domain/ExerciseCatalog.findById(id) → Optional<Exercise>` (FOR-24). No new
persisted entity.

## Edge Cases

- `{type}` not found → 404 via the standard error handler.
- Exercise id not in the catalog → response falls back to the id as the name
  (should not happen given catalog integrity; handle defensively).

## Open Questions

- One list endpoint vs list + by-type — recommend both (`/workouts` and
  `/workouts/{type}`); document.
- Whether to include exercise instructions/equipment/muscles — recommend name (+
  primary muscles) for the MVP table; keep the payload lean. Document.
