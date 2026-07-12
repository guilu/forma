# FOR-105 AI Context

## Story

FOR-105 — Expose nutrition macro totals and target comparison over HTTP
(https://dbhlab.atlassian.net/browse/FOR-105)

## Intent

Make the FOR-32 nutrition calculations reachable over HTTP so the UI shows real
per-meal/day macros + target comparison instead of placeholders. Success is an
enriched nutrition day response with per-meal totals, day totals and the target
comparison, all delegated to the existing calculation service.

## Relevant Documents

- `AGENTS.md`
- `docs/api-conventions.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-32/` (macro calc), `specs/FOR-33/`/`FOR-34/` (day templates/endpoint)
- Jira: https://dbhlab.atlassian.net/browse/FOR-105

## Domain Notes

- `application/NutritionCalculationService`: `mealTotals(MealTemplate)`,
  `dayTotals(List<MealTemplate>)`, `compareToTargets(List<MealTemplate>,
  NutritionDayTemplate)` — reuse; do NOT recompute macros in the mapper/UI.
- `domain/NutritionTotals` = (calories int, proteinG/carbsG/fatG double);
  `domain/TargetComparison` = 4 booleans.
- The existing endpoint is `NutritionController.day(type)` →
  `NutritionDayResponse.from(day)` (FOR-34). `NutritionDay.meals()` returns
  `List<MealTemplate>`; `NutritionDay.template()` is the `NutritionDayTemplate`.
- The calc service is currently NOT injected anywhere in delivery — this story
  wires it into `NutritionController`.

## Architectural Constraints

- Thin controller (ADR-001) on `ApiPaths.V1`. Enrich `NutritionDayResponse`
  (distinct from domain, ADR-005). No persistence. Delegate all math to the
  service.

## Common Pitfalls

- Doing macro math (4/4/9 kcal, sums) in the mapper/UI instead of the service.
- Breaking the existing response shape — additions must be additive/backward
  compatible.
- Forgetting to inject the calc service (the mapper's `from` needs it, or compute
  in the controller).

## Suggested Implementation Order

1. Inject `NutritionCalculationService` into `NutritionController`.
2. Extend `NutritionDayResponse` (+ `from(day, calc)`) with per-meal `totals`,
   day `totals`, `targetComparison`.
3. `@WebMvcTest` asserting the new fields for a seeded day (e.g. running).

## Validation

Run `./gradlew test spotlessApply` from `backend/`. Confirm the endpoint returns
per-meal + day macros and the target comparison.
