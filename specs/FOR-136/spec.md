# FOR-136 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-136
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (muscle-worked-map slice). Unblocks FOR-53 heatmap.

## Summary

Derive, for a strength session, which muscles it works and how much, from the session's
exercises' existing muscle data. `GET /api/v1/training/sessions/{sessionId}/muscle-map`.
Pure derivation — no new persistence, no catalog extension.

## Key finding (resolves FOR-104 open question)

`Exercise` **already carries `primaryMuscles`** (`List<String>`, required non-empty). The
FOR-104 open question "does Exercise carry target-muscle data?" is answered **YES** — no
catalog extension is needed. The map derives directly from the session's exercises'
`primaryMuscles`.

## Repository baseline (verified)

- `Exercise(id, name, movementPattern, primaryMuscles: List<String>, equipment, instructions)` — `primaryMuscles` required, non-empty.
- `StrengthWorkoutTemplate` composes exercises (FOR-25) by id; `WorkoutTemplateCatalog`/`WorkoutTemplateService`; `ExerciseCatalog`.
- `WeeklyTrainingScheduleService` maps weekdays to strength `WorkoutType` (Mon PUSH / Wed PULL / Fri LEGS) with stable session ids `<DAY>:<KIND>` (e.g. `MONDAY:STRENGTH`), reusing the shared `WeeklyTrainingDayPolicy` (FOR-128).

## User/System Flow

1. Frontend (Progreso/Entrenamiento heatmap, mockup 3) GETs `GET /api/v1/training/sessions/{sessionId}/muscle-map`.
2. Backend resolves the session id → its strength template → its exercises, aggregates `primaryMuscles`, and returns a muscle-worked map with a load level per muscle.

## Functional Requirements

- Resolve a strength session to its `StrengthWorkoutTemplate` + exercises (reuse `WorkoutTemplateService`/`ExerciseCatalog` + the schedule's session ids / shared policy — do NOT duplicate resolution).
- Aggregate `primaryMuscles` across the session's exercises into a map: muscle → load level (HIGH/MEDIUM/LOW) derived from how many exercises hit that muscle. Document the frequency thresholds.
- `GET /api/v1/training/sessions/{sessionId}/muscle-map` returns the map.
- Non-strength (running/rest) session → empty map, not an error.
- Never fabricate muscles (Exercise guarantees non-empty primaryMuscles; the empty-map case is for non-strength sessions).

## Non-Functional Requirements

- **No migration** — pure derivation from reference exercise data (head stays V18).
- **No duplicated resolution logic** — reuse the schedule/template/catalog services.
- Explainable/auditable from `Exercise.primaryMuscles`.
- Owner-scoped per ADR-002 (reference data; keep the endpoint consistent with the training API).

## Data Model Notes

- New: a `MuscleWorkedMap` read model (muscle → load). No persisted aggregate, no table.
- `sessionId` = the weekly schedule's stable strength session id (`<DAY>:STRENGTH`) OR the workout template id — pick what the repo cleanly supports and document.
- Load level: derive from exercise frequency hitting a muscle (e.g. ≥2 exercises → HIGH, 1 → MEDIUM; document exact thresholds).

## Edge Cases

- Non-strength (running/rest) session id → empty map, 200 (not 404, not error).
- Unknown session id (not in the schedule/catalog) → 404.
- A muscle worked by many exercises → HIGH per the threshold.
- Session with a single exercise → that exercise's muscles at the base load level.

## Open Questions

- `sessionId` scheme: weekly-schedule `<DAY>:STRENGTH` id vs workout-template id — pick + document; must be resolvable to a template deterministically.
- Load-level thresholds (frequency → HIGH/MEDIUM/LOW) — pick sensible defaults + document.
- Whether to also weight by movement pattern or sets/reps — default frequency-only for MVP; document.
