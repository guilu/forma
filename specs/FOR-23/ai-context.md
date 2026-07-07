# FOR-23 AI Context

## Story

FOR-23 — Seed 16-week running progression
(https://dbhlab.atlassian.net/browse/FOR-23)

## Intent

Give the Training engine real content: a conservative 16-week, 3-sessions/week
running plan from ~4 km to ~10 km, built on FOR-22 `RunningPlanSession`, so the
calendar (FOR-26) and summary (FOR-28) have data. Success is a deterministic,
editable plan with the right structure and a gradual long-run build.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training)
- `docs/adr/ADR-003-persistence.md` (if seeded via migration)
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-22/` (the model this seeds), `specs/FOR-16/` (Flyway/migration
  precedent)
- Jira: https://dbhlab.atlassian.net/browse/FOR-23

## Domain Notes

- The plan is a collection of **planned** `RunningPlanSession` entries, not
  logged runs.
- Progression rule: long-run distance increases gradually; easy/quality runs
  stay moderate to protect recovery (business value: build distance without
  overloading).

## Architectural Constraints

- Reuse FOR-22's `RunningPlanSession`; do not redefine it.
- If persisting, add a new Flyway migration after the latest present in
  `backend/src/main/resources/db/migration/` (verify the next free version;
  never edit `V1__baseline.sql` or existing migrations — ADR-003).
- Keep generation/seeding deterministic and in the domain/application/adapter
  layers as appropriate — not in a controller.

## Common Pitfalls

- Wrong totals — must be 16 weeks × 3 sessions.
- A non-monotonic or too-aggressive distance jump.
- A seed that duplicates rows when re-run (not idempotent).
- Baking the plan in so rigidly that a later "edit plan" story is blocked.

## Suggested Implementation Order

1. Decide seed strategy (migration vs. generator) and document it.
2. Define the 16-week progression (weekly session types + long-run distances).
3. Produce the plan deterministically.
4. Add tests/validation for counts and the gradual progression.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend"/"Persistence" rows —
Flyway + H2 test DB already exist, per `specs/FOR-16`).
