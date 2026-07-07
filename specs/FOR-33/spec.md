# FOR-33: Seed nutrition templates by day type

Jira: https://dbhlab.atlassian.net/browse/FOR-33
Epic: FOR-4 Nutrition Planner

## Summary

Seed initial nutrition day templates for RUNNING, STRENGTH and REST days — each
with meals (FOR-31) built from seeded foods (FOR-30) and approximate macro
targets (FOR-29), verified against calculated totals (FOR-32). Defaults are
directional and editable later; not medical prescriptions.

## User/System Flow

1. Three day templates (RUNNING, STRENGTH, REST) are produced as seed/generated
   data, each with its meals.
2. FOR-32 calc confirms each template's totals land near its targets.
3. Later API/frontend surfaces these default plans before the user customizes.

## Functional Requirements

- Produce a RUNNING, a STRENGTH and a REST `NutritionDayTemplate`, each with
  meals (`MealTemplate` + `MealItem`) referencing FOR-30 foods.
- Target direction: protein ~150–170 g/day; calories in a recomposition range;
  **running days place more carbohydrates before training**; **rest days use
  slightly fewer carbohydrates**.
- Each template's computed macros (FOR-32) are approximately in its target
  range.
- Keep defaults editable later; use only seeded `FoodItem`s.
- Do not present the values as medical prescriptions (neutral copy).

## Non-Functional Requirements

- Deterministic seed/generation; additive Flyway migration only if persisted
  (ADR-003).
- Referential integrity: every `MealItem` references an existing FOR-30 food.

## Data Model Notes

Composes FOR-29/FOR-30/FOR-31 and validates with FOR-32. Consistent with the
project's in-code seed precedents (FOR-23 running plan, FOR-24/FOR-25 catalogs).
No new domain type is introduced.

## Edge Cases

- A seeded meal referencing a missing food id — must fail fast (FOR-25 catalog
  precedent).
- A template whose computed macros fall outside a sane band of its targets —
  the seed should be adjusted so totals are approximately on target.
- Running vs. rest carbohydrate difference must be visible in the data (rest <
  running carbs).

## Open Questions

- **Seed strategy**: in-code generation (FOR-23/FOR-24/FOR-25 precedent) vs.
  Flyway seed. Recommend in-code for MVP so templates stay editable without a
  schema; document.
- Exact meal composition/grams per template — implementer's call within the
  target direction; document the chosen values and keep them realistic.
