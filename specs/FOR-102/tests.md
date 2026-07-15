# FOR-102 Test Plan

Strict TDD: write failing tests first at each layer (domain → application → API), then implement.

## Scope

Meal-log and water-intake persistence, day consumption vs target read model, hydration progress, key-nutrient exposure. Plan-side math is reused, not re-tested here beyond integration.

## Domain Tests

- Meal-log aggregate: adding entries accumulates consumed totals correctly.
- Free/ad-hoc entry with provided macros contributes to totals; key nutrients null when not provided.
- Water-intake aggregate: multiple entries in a day sum; never overwrite.
- Consumption comparison reuses `NutritionTotals`/`TargetComparison`; no duplicated formula.

## Application Tests

- Logging a meal for a day with a plan target → consumption read model reports consumed, target and comparison.
- Logging for a day with NO plan target → consumed totals returned, comparison null/omitted, no error.
- Key nutrients: exposed when `FoodItem` carries them, null when the catalog lacks them (no fabrication).
- Hydration progress: total vs `DefaultObjectives.dailyWater`; goal null → progress null.

## API Tests

- `POST /nutrition/log` valid catalog entry → 200/201, day totals updated on subsequent GET.
- `POST /nutrition/log` free entry → accepted; `POST` with neither foodItemId nor macros → 400.
- `POST /nutrition/log` invalid `mealType` → 400 `VALIDATION_ERROR`.
- `GET /nutrition/consumption` before any log → 200 with empty entries / zero consumed.
- `POST /nutrition/hydration` negative volume → 400; valid → reflected in hydration GET.
- Response shape matches `api.md` (explicit null key nutrients present, not omitted).

## Edge Cases

- Day with logs but unset hydration goal → total volume with null goal/progress.
- Editing/deleting an entry (if in slice scope) recomputes totals.
- Far-future date → 400.

## Fixtures

- A `FoodItem` with full key nutrients and one without, to exercise null-vs-value paths.
- A day with an existing plan target (`NutritionDayTemplate`) and a day without.
- H2-in-PostgreSQL-mode with Flyway migrations for persistence integration tests, matching existing repository test style.
