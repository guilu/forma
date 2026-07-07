# FOR-23: Seed 16-week running progression

Jira: https://dbhlab.atlassian.net/browse/FOR-23
Epic: FOR-3 Training Engine

## Summary

Produce an initial 16-week running plan built from `RunningPlanSession`
(FOR-22): 3 sessions per week (one easy, one quality/controlled, one longer
progressive run), starting near a 4 km baseline and building gradually towards
10 km. Paces/effort stay conservative; the plan must be editable/replaceable
later.

## User/System Flow

1. The plan is produced as seed data (or an equivalent deterministic
   generator) at application start / via a migration.
2. Later, an application/API layer (FOR-26 calendar, and an endpoint noted as a
   future concern) reads the plan to show the week's sessions.

## Functional Requirements

- Produce 16 weeks of planned sessions using FOR-22 `RunningPlanSession`.
- Exactly **3 running sessions per week**: one `EASY`, one quality/controlled
  (`INTERVALS` or a controlled run), one longer progressive run (`LONG_RUN`).
- Long-run distance **progresses gradually** across the 16 weeks, from ~4 km
  towards ~10 km — no large week-to-week jumps.
- Keep effort/pace conservative in this first version; prefer `targetEffort`
  (RPE) over precise paces (FOR-22).
- The plan must be **editable/replaceable later** — do not hardcode it in a way
  that blocks a future "edit plan" story.
- The plan must be retrievable so a later API endpoint can return it (no
  endpoint is created by this story — see Open Questions).

## Non-Functional Requirements

- Deterministic: seeding/generation yields the same 16-week plan every run.
- Additive persistence only (if a Flyway seed migration is used), never
  altering existing migrations (ADR-003).

## Data Model Notes

Builds only on FOR-22 `RunningPlanSession`. If persisted, add a Flyway migration
after the latest one in the repo (check the next free `V<N>__…` — the body
context already added `V2__body_measurements.sql`). No new domain type is
introduced.

## Edge Cases

- Off-by-one in week/session counts — must be exactly 16 weeks × 3 sessions =
  48 planned sessions.
- Non-monotonic long-run distance (a week's long run shorter than the prior) —
  should not happen in the seeded progression.
- Re-running the seed must not duplicate the plan (idempotent seed / migration
  history).

## Open Questions

- **Seed strategy**: Flyway seed migration vs. deterministic in-code generation.
  Recommend whichever keeps the plan editable later without a schema rewrite;
  document the choice. Persistence is not mandated by the AC beyond "returnable
  by an API endpoint later".
- Exact per-week distances/effort curve is not specified — pick a conservative,
  documented progression (e.g. long run 4 → 10 km over 16 weeks) and record it
  in code/tests.
