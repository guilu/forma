# FOR-33 AI Context

## Story

FOR-33 — Seed nutrition templates by day type
(https://dbhlab.atlassian.net/browse/FOR-33)

## Intent

Give the Nutrition planner real default content: RUNNING/STRENGTH/REST day
templates with meals and on-target macros, so the user has useful plans before
customizing. Success is three deterministic templates whose FOR-32 totals land
near their targets, with running days front-loading carbohydrates.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Nutrition)
- `docs/ui-guidelines.md` (calm/neutral tone; "Late running nutrition UX")
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-003-persistence.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-29/`, `specs/FOR-30/`, `specs/FOR-31/`, `specs/FOR-32/`
- `specs/FOR-23/`, `specs/FOR-24/`, `specs/FOR-25/` (in-code seed precedents)
- Jira: https://dbhlab.atlassian.net/browse/FOR-33

## Domain Notes

- Templates compose FOR-31 meals of FOR-30 foods, attached to FOR-29 day
  targets; totals verified with FOR-32.
- Direction only (protein ~150–170 g, recomposition calories, running carbs
  earlier, rest carbs lower) — not a medical prescription.

## Architectural Constraints

- Reuse FOR-29/30/31 types; do not redefine them.
- Prefer in-code seed/generation with fail-fast referential integrity against
  the FOR-30 catalog (FOR-25 `WorkoutTemplateCatalog` precedent).
- If persisting, add a Flyway migration after the latest present (never edit
  existing migrations — ADR-003).

## Common Pitfalls

- Referencing foods not in the FOR-30 catalog.
- Totals that miss the target band (adjust grams so FOR-32 totals are close).
- No visible running-vs-rest carbohydrate difference.
- "Prescription"-style copy.

## Suggested Implementation Order

1. Decide seed strategy (in-code vs. migration) and document it.
2. Build the RUNNING, STRENGTH and REST templates with meals + grams.
3. Use FOR-32 to check each template's totals against its targets; adjust.
4. Tests: three templates exist, referential integrity, totals within band,
   running carbs > rest carbs.

## Validation

Run `./gradlew test` from `backend/`.
