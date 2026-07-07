# FOR-30: Create food item catalog

Jira: https://dbhlab.atlassian.net/browse/FOR-30
Epic: FOR-4 Nutrition Planner

## Summary

Create the `FoodItem` domain model (per-100 g nutrition values + default serving)
plus an initial catalog of common foods used by the plan. `FoodItem` is a
nutrition record, **not** a store product — product/brand data stays out (that is
the Shopping context). Base for meal templates (FOR-31), macro calc (FOR-32) and
shopping estimates (FOR-5).

## User/System Flow

1. `FoodItem` definitions are created (domain model) and an initial set is
   seeded.
2. FOR-31 `MealItem`s reference food items by id.
3. FOR-32 uses per-100 g values + `MealItem` grams to compute macros.

## Functional Requirements

- Add a `FoodItem` domain type under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md "FoodItem".
- Fields: `name`, `kcalPer100g`, `proteinPer100g`, `carbsPer100g`,
  `fatPer100g`, `defaultServingG`.
- Initial catalog includes the plan's common foods: oats, eggs, yogurt, fresh
  cheese, chicken, turkey, fish, rice, potatoes, banana, vegetables, whey
  protein.
- Product-specific fields (price, brand, store url) are **not** part of
  `FoodItem` — keep the food catalog and product catalog separate
  (docs/domain-model.md; Shopping is FOR-5).
- Use sensible estimated per-100 g values for the MVP seed.

## Non-Functional Requirements

- Deterministic seed; additive Flyway migration only if persisted (ADR-003).
- No user PII; the catalog is reference data.

## Data Model Notes

Mirrors docs/domain-model.md's `FoodItem`. Each item references by a stable id so
FOR-31 meal items can point to it (same pattern as the FOR-24 exercise catalog).

## Edge Cases

- Negative nutrition values or non-positive `defaultServingG` — reject/validate.
- Duplicate food names/ids in the seed — decide uniqueness (recommend unique
  ids).
- Estimated values that are clearly wrong (e.g. protein > 100 g per 100 g) —
  keep seed values sane.

## Open Questions

- **Persistence**: in-code catalog (FOR-24 precedent) vs. Flyway seed table.
  Recommend in-code with stable ids for the MVP, so FOR-31 can reference items;
  document the choice. Revisit if Shopping (FOR-5) needs a queryable food table.
- Exact estimated macro values per food are the implementer's call — keep them
  realistic and documented; they are not medical data.
