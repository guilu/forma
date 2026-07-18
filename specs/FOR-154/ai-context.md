# FOR-154 AI Context

## Story

FOR-154 — Plantillas de fuerza reales (5 ej/bloque + RIR/reps). Slice 6 of epic FOR-148. In-code
catalog change; runs after FOR-151 (strength days Mar/Jue/Dom).

## Intent

Make the strength templates match Diego's real Fuerza sheet: 5 exercises per block with per-exercise
series/reps/RIR/rest, adding the missing exercises and modeling AMRAP and time-based holds. Success =
the three templates and the exercise catalog reflect the sheet; the FOR-136 muscle-map still derives
from `primaryMuscles`.

## Relevant Documents

- `AGENTS.md` — hexagonal, deterministic domain, no fabricated data.
- `docs/fitness_os.xlsm` — sheet **Fuerza** (3 blocks × 5 exercises, per-exercise programming). Source of truth.
- `docs/adr/ADR-001-architecture.md` (domain-owned, no logic in UI), `ADR-002-authentication.md` (owner-scoping), `ADR-003-persistence.md` (no migration).
- Upstream: `specs/FOR-151/` (strength days). Related: `specs/FOR-136/` (muscle-map reads `primaryMuscles`).
- Jira: https://dbhlab.atlassian.net/browse/FOR-154

## Domain / Repo Notes (verified)

- `WorkoutTemplateCatalog` — 3 blocks, uniform `SETS=3, RIR=2`, rest 90/45s, 3 ex/block; fail-fast on unknown `exerciseId`.
- `ExerciseCatalog` — 11 exercises; missing dumbbell bench press, lateral raise, biceps curl, rear-delt fly, calf raise.
- `StrengthWorkoutItem(exerciseId, order, sets, repsMin, repsMax, restSeconds, rir)` — **RIR and rest already exist per item**; `repsMin/repsMax` ints, `repsMax≥repsMin`, all ≥1. No AMRAP, no time-hold.
- `Exercise(id, name, movementPattern, primaryMuscles, equipment, instructions)` — `primaryMuscles` required non-empty (feeds FOR-136).
- In-code, no migration (head V19). Strength days come from `WeeklyTrainingDayPolicy` (FOR-151).

## Architectural Constraints

- Minimal, backward-compatible extension of `StrengthWorkoutItem` for AMRAP/time schemes; keep the range case working.
- New exercises need real `primaryMuscles` (the muscle-map depends on them) and home `Equipment`.
- Pure, framework-free (ADR-001); no migration (ADR-003); keep `WorkoutType` PUSH/PULL/LEGS.
- Preserve the fail-fast exercise-id check.

## Common Pitfalls

- Assuming RIR/rest must be added to `StrengthWorkoutItem` — they already exist; the real gap is AMRAP + time-based holds.
- Forcing AMRAP/45–75s into `repsMin/repsMax` (breaks invariants / lies about the prescription).
- Adding exercises without `primaryMuscles` (breaks the Exercise invariant and the FOR-136 map).
- Fabricating programming values not in the Fuerza sheet.
- Renaming `WorkoutType` values or hardcoding strength days here (days come from FOR-151).

## Suggested Implementation Order

1. Add the ~5 missing exercises to `ExerciseCatalog` (ids + `primaryMuscles` + equipment + instructions).
2. Extend `StrengthWorkoutItem` minimally for AMRAP + time-based holds (RepScheme/kind + optional fields), keeping range behavior.
3. Rebuild the 3 templates with 5 exercises each and per-exercise sets/reps/RIR/rest from the Fuerza table.
4. Keep the fail-fast id check; update catalog/template tests.

## Validation

Backend build + tests (`./gradlew build`). Confirm: 3 blocks × 5 exercises with per-exercise programming matching the Fuerza sheet; new exercises present with `primaryMuscles`; AMRAP and 45–75s hold represented without breaking range invariants; fail-fast id check intact; FOR-136 muscle-map still derives; no migration (head V19); strength days unchanged (FOR-151).
