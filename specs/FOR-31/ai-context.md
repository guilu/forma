# FOR-31 AI Context

## Story

FOR-31 — Create meal template model
(https://dbhlab.atlassian.net/browse/FOR-31)

## Intent

Turn day-type targets (FOR-29) and foods (FOR-30) into reusable meals. Success is
`MealTemplate` + `MealItem` domain types with a constrained `mealType`, meals
composed of catalog foods in grams, belonging to a day template.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Nutrition → MealTemplate, MealItem)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-29/`, `specs/FOR-30/` (the day template + food catalog these use)
- `specs/FOR-25/` (StrengthWorkoutTemplate/Item — the template+item precedent)
- Jira: https://dbhlab.atlassian.net/browse/FOR-31

## Domain Notes

- `MealItem` references a `FoodItem` by **catalog id** (FOR-30); it does not
  embed nutrition values. Macro calc (FOR-32) resolves the food to compute
  totals.
- `mealType` is a closed set → enum. A meal belongs to a `NutritionDayTemplate`
  (FOR-29) — a running/strength/rest day meal.

## Architectural Constraints

- Types in `.../domain/`, framework-free (ADR-001), no ORM.
- Follow the FOR-25 `StrengthWorkoutTemplate` + `StrengthWorkoutItem` shape
  (template holds ordered items; items reference a catalog entry by id).
- Referential integrity (food ids exist) is enforced where meals are built/
  seeded (FOR-33), not necessarily by the type itself.

## Common Pitfalls

- Embedding `FoodItem` nutrition values into `MealItem` instead of referencing
  by id.
- Free-form `mealType` string instead of an enum.
- Coupling the meal to persistence (none is introduced here).

## Suggested Implementation Order

1. Define the `MealType` enum, `MealItem` and `MealTemplate` records.
2. Add construction-time validation (positive `quantityG`, required refs).
3. Decide the day-template reference shape (type vs id) and `preferredTime`
   type; document both.
4. Unit-test creation, the constrained `mealType`, and multi-item meals.

## Validation

Run `./gradlew test` from `backend/`.
