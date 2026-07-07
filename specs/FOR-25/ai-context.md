# FOR-25 AI Context

## Story

FOR-25 — Create strength workout templates
(https://dbhlab.atlassian.net/browse/FOR-25)

## Intent

Turn the FOR-24 exercise catalog into usable, moderate-volume home workouts
(Push / Pull / Legs & core) that complement running. Success is three templates
of ordered, catalog-referenced items with sets/reps/rest/effort.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training → StrengthWorkout, StrengthWorkoutItem)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-003-persistence.md` (if seeded)
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-24/` (exercise catalog these templates reference)
- Jira: https://dbhlab.atlassian.net/browse/FOR-25

## Domain Notes

- A template references exercises by **catalog id** (FOR-24); it does not embed
  exercise definitions.
- Distinguish the reusable **template** from a **scheduled/completed session**
  (FOR-26/FOR-27 own scheduling + status). Keep the template free of
  `date`/`status`.

## Architectural Constraints

- Types in `.../domain/`, framework-free (ADR-001), JDBC + Flyway if persisted
  (no ORM).
- New Flyway migration after the latest present (verify next free version;
  never edit existing migrations — ADR-003).

## Common Pitfalls

- Referencing exercises not in the FOR-24 catalog.
- Baking `date`/`status` into the reusable template.
- Over-engineering periodization (explicitly out of scope for MVP).
- Free-form `workoutType` instead of the documented enum.

## Suggested Implementation Order

1. Model `StrengthWorkout` (template) + `StrengthWorkoutItem`.
2. Decide template-vs-instance split and persistence; document both.
3. Seed the three templates with ordered items (sets/reps/rest/rir).
4. Add tests: catalog references valid, each item has sets/reps/rest, three
   templates exist.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend"/"Persistence" rows).
