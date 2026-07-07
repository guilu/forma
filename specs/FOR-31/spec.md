# FOR-31: Create meal template model

Jira: https://dbhlab.atlassian.net/browse/FOR-31
Epic: FOR-4 Nutrition Planner

## Summary

Create the `MealTemplate` and `MealItem` domain models: reusable meals (each with
a meal type, name, preferred time and food items in grams) that belong to a
`NutritionDayTemplate` (FOR-29) and reference `FoodItem`s (FOR-30). Domain-only.

## User/System Flow

This story has no user-facing flow. It defines the types consumed by later
stories:

1. FOR-32 computes macros from `MealItem` grams + `FoodItem` per-100 g values.
2. FOR-33 seeds day templates composed of these meals.
3. FOR-34 arranges the running-day meal flow using these meals.

## Functional Requirements

- Add `MealTemplate` and `MealItem` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md.
- `MealTemplate` fields: reference to a `NutritionDayTemplate` (day type),
  `mealType`, `name`, `preferredTime`, `notes`.
- `MealItem` fields: reference to a `FoodItem` (by id), `quantityG`.
- `mealType` constrained to: `BREAKFAST`, `MID_MORNING`, `LUNCH`,
  `PRE_WORKOUT`, `POST_WORKOUT`, `DINNER` (enum).
- A meal can contain **multiple** `MealItem`s.
- A meal **belongs to** a nutrition day template (references FOR-29's type/id).
- Quantities are explicit in grams; templates are reusable and editable later.

## Non-Functional Requirements

- Deterministic; framework-free. No persistence introduced unless a story asks.
- No user PII.

## Data Model Notes

Mirrors docs/domain-model.md's `MealTemplate` + `MealItem`. `MealItem` references
`FoodItem` **by id** (FOR-30), not by embedding nutrition values (those are read
during calc, FOR-32). The day-template reference ties a meal to a `RUNNING` /
`STRENGTH` / `REST` day.

## Edge Cases

- A `MealItem` referencing a non-existent `FoodItem` id — must be rejected
  (enforced where meals are built/seeded, FOR-33).
- `quantityG` zero or negative — reject at construction.
- An empty meal (no items) — decide validity (recommend at least one item for a
  seeded meal; document).

## Open Questions

- **Day-template reference shape**: reference by the `NutritionDayType` enum vs.
  a template id. Since FOR-29 templates may be identified by type in the MVP,
  referencing by type is simplest; document the choice and keep it consistent
  with FOR-33 seeding.
- Whether `preferredTime` is a `java.time.LocalTime` or a simple label —
  recommend `LocalTime` for structure; document.
