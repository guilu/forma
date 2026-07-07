# FOR-32 AI Context

## Story

FOR-32 — Implement nutrition macro calculation
(https://dbhlab.atlassian.net/browse/FOR-32)

## Intent

Let the app know whether a meal/day hits its calorie and protein targets without
manual math. Success is deterministic, tested calculation of the four macros for
meals and days, plus a targets comparison — no forecasting, no recommendations.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Nutrition)
- `docs/ui-guidelines.md` ("no fake precision")
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-29/`, `specs/FOR-30/`, `specs/FOR-31/`
- `specs/FOR-21/`, `specs/FOR-28/` (pure domain-calc + thin service precedents)
- Jira: https://dbhlab.atlassian.net/browse/FOR-32

## Domain Notes

- Core formula: `foodItem.<macro>Per100g * mealItem.quantityG / 100`, summed.
- Comparison is descriptive (reached/short per macro), not prescriptive —
  recommendations are the Insights context (FOR-6), not here.

## Architectural Constraints

- Pure calculation in the domain (a `NutritionCalculator` / totals value type),
  wrapped by a thin application service if a seam is useful — like FOR-21
  `WeeklyBodySummaryService` / FOR-28.
- Resolve `foodItemId` via the FOR-30 catalog; do not duplicate food values.
- No controller logic; no persistence.

## Common Pitfalls

- Rounding each item then summing (accumulated error) — sum raw, round only for
  display.
- Silently skipping an unknown `foodItemId` and understating totals.
- Building the Insights/recommendation engine here (out of scope).

## Suggested Implementation Order

1. Define a `NutritionTotals` value type (calories, protein, carbs, fat).
2. Implement meal totals, then day totals (sum of meals).
3. Add a day-vs-targets comparison (per-macro reached/short).
4. Tests: known meal/day totals, empty case, unknown food id, targets
   comparison.

## Validation

Run `./gradlew test` from `backend/`.
