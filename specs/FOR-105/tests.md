# FOR-105 Test Plan

## Scope

Verify the nutrition day endpoint now returns per-meal and per-day macro totals
and the target comparison, delegated to the FOR-32 service.

## Domain Tests

N/A — macro calc covered by FOR-32.

## Application Tests

N/A — reuses `NutritionCalculationService`.

## API Tests

- `GET /api/v1/nutrition/days/{type}` (e.g. `running`) returns, in addition to the
  existing `targets` + `meals`:
  - each meal's `totals` (calories, proteinG, carbsG, fatG),
  - the day `totals`,
  - `targetComparison` (caloriesReached/proteinReached/carbsReached/fatReached).
- The existing fields (`type`, `targets`, `meals` with items) are unchanged
  (backward compatible).
- Unknown day type → 404 (unchanged).

## UI Tests

N/A — backend story (FOR-54 consumes it).

## Edge Cases

- A day with an optional post-workout meal → its `totals` are present.
- Values match the FOR-32 service output (no re-rounding in the mapper).

## Fixtures

- The seeded nutrition days (FOR-33) via `NutritionDayCatalogService`.
