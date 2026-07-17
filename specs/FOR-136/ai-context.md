# FOR-136 AI Context

## Story

FOR-136 — Muscle-worked map for strength sessions. Muscle-worked-map slice of FOR-104 [STUB] Progress & goals domain. Unblocks FOR-53 heatmap.

## Intent

Derive a strength session's worked-muscle map from its exercises' existing muscle data. Success = `GET /api/v1/training/sessions/{sessionId}/muscle-map` returns muscles + load, feeding the FOR-53 heatmap. Pure derivation; no persistence.

## Key finding (verified)

`Exercise` **already carries `primaryMuscles`** (`List<String>`, required non-empty) — the FOR-104 open question "does Exercise carry target-muscle data?" is answered YES. **No catalog extension needed.**

## Relevant Documents

- `specs/FOR-104/` — full scope; the muscle-map open question is resolved here.
- `AGENTS.md` — hexagonal, explainable, no duplicated logic.
- `docs/adr/ADR-001-architecture.md`, `ADR-005-api-design.md`.
- Mockup: `docs/3-entrenamiento.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-136

## Domain / Repo Notes (verified)

- `Exercise(id,name,movementPattern,primaryMuscles,equipment,instructions)` — `primaryMuscles: List<String>`, required non-empty.
- `StrengthWorkoutTemplate` composes exercises by id (FOR-25); `WorkoutTemplateCatalog`/`WorkoutTemplateService`; `ExerciseCatalog`.
- `WeeklyTrainingScheduleService` maps weekdays → strength `WorkoutType` (Mon PUSH/Wed PULL/Fri LEGS), stable session ids `<DAY>:<KIND>`, reusing `WeeklyTrainingDayPolicy` (FOR-128).
- The `training` delivery package + `ProgressController` (FOR-129/135) are the API surfaces; add the muscle-map endpoint where it fits (training sessions path).

## Architectural Constraints

- Pure derivation: a `MuscleWorkedMap` read model + a service that resolves session → template → exercises → aggregate `primaryMuscles`. NO persistence, NO migration (head stays V18).
- Reuse the schedule/template/catalog services to resolve the session — do NOT duplicate that logic; avoid a nutrition↔training-style cycle (reuse the shared policy).
- Load level from exercise-frequency thresholds (documented). Owner-scoped per ADR-002, consistent with the training API.

## Common Pitfalls

- Extending the Exercise catalog with muscle data — it already has `primaryMuscles`; don't add a table/field.
- Duplicating the schedule/template resolution logic instead of reusing `WorkoutTemplateService`/`WeeklyTrainingScheduleService`.
- Returning 404 (or an error) for a non-strength session — return an empty map.
- Fabricating muscles — derive only from real `primaryMuscles`.
- Adding a migration for what is pure derivation.
- Basing anything on per-date training completion history (unrelated gap).

## Suggested Implementation Order

1. `MuscleWorkedMap` read model + aggregation (union of `primaryMuscles`, load by frequency) — pure, tested with fixtures + threshold boundaries.
2. Application service: resolve `sessionId` → strength template → exercises (reuse services), aggregate; empty for non-strength.
3. Delivery: `GET /api/v1/training/sessions/{sessionId}/muscle-map` + DTO (+ API tests), 404 on unknown id.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: map derives from real `Exercise.primaryMuscles`; load thresholds hold at boundaries; non-strength session → empty map (200); unknown id → 404; no catalog extension; no duplicated resolution; no migration (head V18); no training schedule/summary regression.
