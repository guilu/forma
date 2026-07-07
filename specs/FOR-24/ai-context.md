# FOR-24 AI Context

## Story

FOR-24 — Create strength exercise catalog
(https://dbhlab.atlassian.net/browse/FOR-24)

## Intent

Provide the building blocks for home strength training: an `Exercise` catalog
that FOR-25 templates compose. Success is a constrained, home-equipment-only set
of exercises covering push/pull/legs/core, referenceable by later templates.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training → Exercise)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-003-persistence.md` (if seeded via migration)
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/`, `specs/FOR-16/` (domain + Flyway precedents)
- Jira: https://dbhlab.atlassian.net/browse/FOR-24

## Domain Notes

- `movementPattern` and `equipment` are closed sets → model as Java `enum`s
  (like FOR-15 `MeasurementSource`).
- The catalog is **reference data**, not user data — no ownership/scoping.

## Architectural Constraints

- `Exercise` type in `.../domain/`, framework-free (ADR-001), JDBC + Flyway (no
  ORM) if persisted.
- New Flyway migration goes after the latest in
  `backend/src/main/resources/db/migration/` (verify next free version; never
  edit existing migrations — ADR-003).

## Common Pitfalls

- Adding machine-only or gym-only exercises (violates the home-equipment
  constraint).
- Free-form strings for `movementPattern`/`equipment` instead of enums.
- Adding fields not in docs/domain-model.md's `Exercise`.

## Suggested Implementation Order

1. Define `MovementPattern` and `Equipment` enums and the `Exercise` type.
2. Decide persistence (migration vs. in-code) and document it.
3. Seed push/pull/legs/core exercises with home equipment only.
4. Add tests/validation for the seed coverage and equipment constraint.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend"/"Persistence" rows).
