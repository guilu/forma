# FOR-32: Implement nutrition macro calculation

Jira: https://dbhlab.atlassian.net/browse/FOR-32
Epic: FOR-4 Nutrition Planner

## Summary

Implement rule-based calculation of nutrition totals (calories, protein, carbs,
fat) for a **meal** and for a **full nutrition day template**, from `FoodItem`
per-100 g values (FOR-30) and `MealItem` grams (FOR-31), and allow comparing day
totals to the day's targets (FOR-29). Domain calculation; no persistence,
mirroring the FOR-21/FOR-28 summary precedents.

## User/System Flow

This story has no direct user flow. It produces totals consumed later:

1. A meal's totals = sum over its items of `foodItem.<macro>Per100g *
   quantityG / 100`.
2. A day template's totals = sum over its meals.
3. Day totals can be compared to the `NutritionDayTemplate` targets (FOR-29) to
   show whether calories/protein are on target.

## Functional Requirements

- Compute `calories`, `proteinG`, `carbsG`, `fatG` for a meal (`MealTemplate`)
  and for a day (its meals).
- Use `FoodItem` per-100 g values × `MealItem` `quantityG / 100`.
- Round values sensibly for display (no fake precision — docs/ui-guidelines.md).
- Provide a comparison of day totals vs. day targets (FOR-29) — e.g. per-macro
  reached/short — without prescribing action (that is Insights, FOR-6).
- Keep the calculation in the domain/application layer (ADR-001), pure and
  testable — no controller logic.

## Non-Functional Requirements

- Deterministic: same meals/foods always produce the same totals.
- Performance: in-memory sums; O(items).

## Data Model Notes

Builds on FOR-30 (`FoodItem`) and FOR-31 (`MealTemplate`/`MealItem`). Introduces a
totals value type (e.g. `NutritionTotals` with the four macros). No persisted
entity. Resolving a `MealItem`'s `foodItemId` to a `FoodItem` uses the FOR-30
catalog.

## Edge Cases

- A meal/day with no items — totals are all zero (valid), not an error.
- A `MealItem` whose `foodItemId` is unknown in the catalog — decide (reject vs.
  skip); recommend rejecting so totals are never silently understated.
- Rounding: display rounding must not accumulate error across sums (sum raw,
  round for display).

## Open Questions

- **Exposure**: totals are consumed by API or frontend later (Jira DoD). No
  endpoint is defined by this story — recommend a pure calculation + a value
  type now, wiring an endpoint/frontend in a later/explicit step.
- Exact rounding rule (whole kcal, one-decimal grams) — pick and document,
  consistent with docs/ui-guidelines.md "no fake precision".
