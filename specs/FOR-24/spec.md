# FOR-24: Create strength exercise catalog

Jira: https://dbhlab.atlassian.net/browse/FOR-24
Epic: FOR-3 Training Engine

## Summary

Create the initial catalog of home-friendly strength exercises: an `Exercise`
domain model (name, movement pattern, primary muscles, equipment, instructions)
plus an initial seed covering push, pull, legs and core. Exercises must be
usable by strength workout templates (FOR-25). No gym/machine-only exercises.

## User/System Flow

1. `Exercise` definitions are created (domain model) and an initial set is
   seeded.
2. FOR-25 workout templates reference catalog exercises by id.
3. FOR-26 calendar / FOR-27 completion surface the resulting workouts.

## Functional Requirements

- Add an `Exercise` domain type under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md "Exercise".
- Fields: `name`, `movementPattern`, `primaryMuscles`, `equipment`,
  `instructions`.
- `movementPattern` constrained to: `PUSH`, `PULL`, `SQUAT`, `HINGE`, `CORE`
  (docs/domain-model.md also lists `CARRY`; include only if a seeded exercise
  needs it, otherwise document the gap — do not invent scope).
- `equipment` constrained to home equipment: `DUMBBELL`, `BENCH`, `BAND`,
  `PULL_UP_BAR`, `BODYWEIGHT` (docs/domain-model.md).
- Initial seed includes push, pull, legs (squat/hinge) and core exercises;
  each declares its required `equipment`.
- Neutral exercise names; concise instructions; **no machine-only exercises**.

## Non-Functional Requirements

- Deterministic seed; additive Flyway migration only if persisted (ADR-003).
- Security/observability: none specific; do not log user data (catalog is
  reference data).

## Data Model Notes

Mirrors docs/domain-model.md's `Exercise`. `primaryMuscles` is a small list; how
it is stored (if persisted) is an implementation detail (see Open Questions). No
user-specific data lives in the catalog.

## Edge Cases

- An exercise whose equipment isn't in the supported home set — must be
  rejected/excluded (no gym machines).
- Duplicate exercise names in the seed — decide whether names must be unique.
- Empty `primaryMuscles` or `instructions` — decide required vs optional.

## Open Questions

- **Persistence**: seed as a Flyway migration (new `V<N>__…`, after the latest
  in the repo) vs. in-code catalog. Recommend whichever lets FOR-25 reference
  exercises by stable id; document the choice.
- Whether `movementPattern` should include `CARRY` now — only if a seeded
  exercise uses it; otherwise leave it out and note it as available later.
