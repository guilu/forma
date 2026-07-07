# FOR-32 Test Plan

## Scope

Verify nutrition macro calculation for meals and days, and the day-vs-targets
comparison.

## Domain Tests

- A meal's totals are computed correctly from item foods × grams
  (`per100g * g / 100`).
- A day template's totals equal the sum of its meals.
- Totals are rounded for display without accumulated error (sum raw, round
  once).
- Day totals compared to targets report per-macro reached/short correctly.

## Application Tests

- The calculation service (if introduced) resolves food ids via the FOR-30
  catalog and returns complete totals for a realistic day.

## API Tests

N/A — no HTTP endpoint is defined by this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Empty meal / empty day → all-zero totals (not an error).
- `MealItem` with an unknown `foodItemId` → rejected (per the documented rule),
  so totals are never silently understated.
- Boundary grams (e.g. exactly 100 g → equals the per-100 g values).

## Fixtures

- A meal with two foods and known grams whose totals are hand-computable.
- A day template with two meals (to assert day = sum of meals).
- A `NutritionDayTemplate` with targets to assert the comparison.
