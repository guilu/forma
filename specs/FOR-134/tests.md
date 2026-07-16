# FOR-134 Test Plan

Strict TDD: failing tests first (FoodItem validation → calculator → consumption read model → API), then implement. No migration.

## Scope

Key-nutrient reference data on `FoodItem` + catalog, and consumed key-nutrient totals in the day consumption read model. Macros/hydration/day-type are out of scope (done).

## Domain Tests

- `FoodItem` accepts nullable fiber/sugars/sodium/saturatedFat; non-negative when present; negative → throws.
- A `FoodItem` with null key nutrients is valid (reference data incomplete).
- The 12 catalog entries construct successfully; entries with known values expose them, unknowns are null (no fabricated numbers — assert a couple of known-null cases).

## Calculator Tests

- Consumed key nutrients scale by grams like macros (reuse `NutritionCalculator`).
- The documented null/partial-total rule holds: a day mixing foods with and without a nutrient totals per the chosen rule (assert exactly).
- No duplicated macro math — macro totals unchanged vs FOR-127.

## API Tests

- `GET /nutrition/consumption` after logging catalog foods → `keyNutrients` totals present with correct units (sodium mg, others g).
- Logging a food with null fiber → fiber total follows the documented rule.
- Empty day → key-nutrient totals zero/null per rule, never 404.
- `POST /nutrition/log` free entry with key nutrients → reflected in the day total; negative key nutrient → 400.
- FOR-127 regression: macro consumption + logging unchanged.

## Edge Cases

- Food with no key data → contributes null (no fabrication).
- Free entry without key nutrients → null.
- Mixed day (some foods have sodium, some don't) → documented total.

## Fixtures

- Catalog `FoodItem`s: one fully populated, one with all key nutrients null, one partial.
- A logged day mixing them.
- H2-in-PostgreSQL-mode with Flyway (no new migration; head stays V16) for the API/persistence path, matching FOR-127 test style.
