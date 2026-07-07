# FOR-31 Test Plan

## Scope

Unit-test `MealTemplate` and `MealItem`: creation, the constrained `mealType`,
multi-item meals, and construction validation.

## Domain Tests

- A `MealTemplate` with multiple `MealItem`s is created correctly.
- `mealType` only accepts the known values (enum).
- Each `MealItem` carries a `foodItemId` reference and a positive `quantityG`.
- Construction rejects invalid values (non-positive `quantityG`, missing refs).
- A meal is associated with a nutrition day template (type/id reference present).

## Application Tests

N/A — no application/use-case layer is introduced by this story (referential
integrity against the FOR-30 catalog is exercised when meals are seeded in
FOR-33).

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- `MealItem` with zero/negative grams.
- Empty meal (no items) — per the documented validity decision.
- Duplicate food items within a meal — decide and document.

## Fixtures

- A breakfast meal with two food items.
- A single-item pre-workout snack meal.
