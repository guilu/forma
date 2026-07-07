# FOR-25: Create strength workout templates

Jira: https://dbhlab.atlassian.net/browse/FOR-25
Epic: FOR-3 Training Engine

## Summary

Create the initial strength workout templates — Push, Pull, and Legs & core —
each composed of ordered exercises from the FOR-24 catalog with sets, rep range,
rest time and target effort (reps in reserve). Moderate volume suitable for
someone also running; no complex periodization in the MVP.

## User/System Flow

1. Templates are created referencing FOR-24 `Exercise` entries by id.
2. FOR-26 calendar shows the planned strength session (template) on its day.
3. FOR-27 lets a strength session be marked completed/skipped.

## Functional Requirements

- Create a `StrengthWorkout` template model + `StrengthWorkoutItem` per
  docs/domain-model.md ("StrengthWorkout", "StrengthWorkoutItem"), framework-free
  (ADR-001).
- Three templates: **Push**, **Pull**, **Legs & core**. `workoutType` uses the
  docs/domain-model.md set (`PUSH`, `PULL`, `LEGS`, `FULL_BODY`); map "Legs and
  core" → `LEGS` and document the mapping (no new enum value unless needed).
- Each template contains **ordered** `StrengthWorkoutItem`s; each item includes:
  `exerciseId` (FOR-24 catalog), `sets`, rep range (`repsMin`/`repsMax`),
  `restSeconds`, and target effort / reps-in-reserve (`rir`).
- Only exercises from the FOR-24 catalog may be referenced (no ad-hoc
  exercises).
- Keep volume moderate (complements running); avoid periodization schemes.

## Non-Functional Requirements

- Deterministic seed; additive Flyway migration only if persisted (ADR-003).
- No user-specific data; templates are reference/plan data.

## Data Model Notes

Mirrors docs/domain-model.md's `StrengthWorkout` + `StrengthWorkoutItem`. Note
`StrengthWorkout` there also carries `status` (PLANNED/COMPLETED) and a `date`;
for a **template** those belong to the scheduled/instance concept used by FOR-26/
FOR-27, not the reusable template — clarify template vs. scheduled instance
during implementation (see Open Questions).

## Edge Cases

- A template item referencing a non-existent catalog exercise — must be
  rejected.
- Empty template (no items) — invalid; each template needs items.
- Item ordering collisions (duplicate `order`) — decide and document.

## Open Questions

- **Template vs. scheduled instance**: docs/domain-model.md's `StrengthWorkout`
  mixes template-like fields with instance fields (`date`, `status`). Decide
  whether FOR-25 models a reusable *template* separately from a scheduled
  session (recommended: template holds exercises/sets/reps; scheduling +
  completion status come with FOR-26/FOR-27). Document the split.
- **Persistence**: Flyway seed migration vs. in-code templates — pick one,
  keep exercises referenced by stable id, document the choice.
