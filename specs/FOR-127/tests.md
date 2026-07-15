# FOR-127 Test Plan

Strict TDD: failing tests first at each layer (domain → application → persistence → API), then implement.

## Scope

Meal-log domain + persistence, and the day consumption vs plan-target read model. Macros only. Hydration and key nutrients are out of scope (later FOR-102 slices). Plan-side math is reused, verified via integration not re-tested in isolation.

## Domain Tests

- Meal-log aggregate: adding catalog + free entries accumulates consumed macros correctly.
- Free/ad-hoc entry with provided macros contributes to totals.
- Consumed totals reuse `NutritionTotals`/`NutritionCalculator`; no duplicated formula.
- Logging does not mutate any plan template.

## Application Tests

- Logging for a day WITH a plan target → consumption read model reports consumed, target and comparison.
- Logging for a day with NO plan target → consumed totals returned, comparison null/omitted, no error.
- Multiple entries same meal/day → all counted, none overwritten.
- Owner-scoping: consumption only reflects the owner's entries.

## Persistence Tests

- Round-trip a day's logged entries through the JDBC adapter against H2-in-PostgreSQL-mode with Flyway (V13).
- Empty DB / empty day → zeroed consumption, no error.

## API Tests

- `POST /nutrition/log` catalog entry → 200/201; subsequent consumption GET reflects it.
- `POST /nutrition/log` free entry → accepted.
- `POST /nutrition/log` with neither foodItemId nor macros → 400; unknown `mealType` → 400; negative portions → 400.
- `GET /nutrition/consumption` before any log → 200 zeroed consumed / empty entries, never 404.
- `GET /nutrition/consumption` day without plan target → consumed present, target/comparison null/omitted.
- Response shape matches `api.md`.

## Edge Cases

- Far-future date → 400.
- Unknown `foodItemId` → 400.
- Day with logs but no plan target → consumed-only read model.

## Fixtures

- A `FoodItem` from the catalog (macros known) plus a free-entry payload.
- A day with an existing plan target (`NutritionDayTemplate`) and a day without.
- H2-in-PostgreSQL-mode with Flyway migrations for persistence/API integration tests, matching FOR-107/110/125/126 test style.
