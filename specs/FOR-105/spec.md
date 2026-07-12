# FOR-105: Expose nutrition macro totals and target comparison over HTTP

Jira: https://dbhlab.atlassian.net/browse/FOR-105
Epic: FOR-95 UI Backend Enablers

## Summary

Enrich the nutrition day read model with the FOR-32 computed macros: per-meal and
per-day macro totals (protein / carbs / fat grams + kcal) and the day's
target-vs-actual comparison. The domain `NutritionCalculationService` already
computes these but is not exposed over HTTP, so the FOR-54 UI cannot show per-meal
macro chips, calorie-based ring proportions, or an objetivo-vs-actual view. Thin
mapping over the existing service — no new domain, no persistence.

## User/System Flow

1. Client calls `GET /api/v1/nutrition/days/{type}` (existing FOR-34 endpoint).
2. The controller maps the seeded `NutritionDay` and, via
   `NutritionCalculationService` (FOR-32), computes per-meal totals, day totals
   and the target comparison, adding them to the response.
3. FOR-54 renders per-meal macros + kcal, a calorie-based macro ring, and the
   objetivo-vs-actual state.

## Functional Requirements

- Extend the existing `GET /api/v1/nutrition/days/{type}` response (additive,
  backward compatible) with:
  - per **meal**: `totals` (calories, proteinG, carbsG, fatG) from
    `NutritionCalculationService.mealTotals(...)`.
  - per **day**: `totals` (day totals from `dayTotals(...)`) and `targetComparison`
    (from `compareToTargets(...)`).
- Wire `NutritionCalculationService` into `NutritionController` and pass it to the
  `NutritionDayResponse` mapper (or compute in the controller). No macro math in
  the mapper beyond delegating to the service (ADR-001).
- Keep the `targets` block already present. DTO stays distinct from domain
  (ADR-005).

## Non-Functional Requirements

- Deterministic, computed on demand; no persistence.
- No fake precision — carry the service's rounded values (calories whole kcal,
  grams one decimal) as-is; the calculator rounds once (no accumulation).

## Data Model Notes

Reuses `application/NutritionCalculationService` (`mealTotals(MealTemplate)`,
`dayTotals(List<MealTemplate>)`, `compareToTargets(List<MealTemplate>,
NutritionDayTemplate)`), `domain/NutritionTotals` (calories int, proteinG/carbsG/
fatG double) and `domain/TargetComparison` (caloriesReached/proteinReached/
carbsReached/fatReached booleans). Existing `NutritionDay` exposes `template()` +
`meals()` (List<MealTemplate>). No new persisted entity.

## Edge Cases

- A day with an optional (post-workout) meal → its totals are included like any
  other meal; the UI decides how to treat the optional flag.
- Empty/absent day type → unchanged 404 (existing behavior).

## Open Questions

- Enrich the existing endpoint vs add `GET /nutrition/days/{type}/macros` —
  recommend **enriching** the existing response (single call, additive fields);
  document. This story assumes enrichment.
- This exposes PLAN macros vs target only — actual *consumed* logging remains the
  FOR-102 (Foundations) stub; document so FOR-54 labels it as plan, not intake.
