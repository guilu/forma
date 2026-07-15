# FOR-102 AI Context

## Story

FOR-102 — [STUB] Nutrition consumption logging + hydration. Epic-sized; must be split into the *Proposed story slices* in `spec.md` before implementation.

## Intent

Move the nutrition domain from **plan-only** to **plan + actual consumption**: record what the user ate and drank, and expose consumed-vs-target and hydration read models. Success = FOR-54 meal logging + hydration + key nutrients, and the dashboard "calories eaten vs target" widget, all have a real backend.

## Relevant Documents

- `AGENTS.md` — hexagonal boundaries, owner-scoping, no logging of sensitive health data.
- `docs/architecture-overview.md`, `docs/glossary.md`, `docs/domain-model.md`.
- `docs/adr/ADR-001-architecture.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`, `ADR-002-authentication.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-102

## Domain Notes

- Existing plan-side domain (do NOT duplicate): `NutritionDay`, `NutritionDayTemplate`, `NutritionDayCatalog`, `NutritionTotals`, `NutritionCalculator`, `TargetComparison`, `FoodItem`, `FoodCatalog`, `MealTemplate`, `MealItem`, `MealType`.
- FOR-105 already exposes macro totals + target comparison over HTTP — reuse that read-model style.
- There is **no** consumption or hydration concept in the repo today — this story introduces both.
- Hydration goal ideally reads `DefaultObjectives.dailyWater` (FOR-107, merged).

## Architectural Constraints

- Domain framework-free; commands via application services; thin controllers under `delivery/nutrition`.
- Logging is additive — never mutate plan templates to represent consumption.
- Reuse `NutritionCalculator`/`NutritionTotals` for all macro math; the frontend must not compute it.
- New migration is **V11 or later** (current head V10); one column per statement (H2/PostgreSQL convention).
- Owner-scoped; never log entry contents or volumes at INFO.

## Common Pitfalls

- Fabricating key nutrients the `FoodItem` catalog does not carry — expose null instead, and flag a prerequisite catalog-extension slice if needed.
- Overwriting hydration instead of summing entries.
- Returning 404 for an empty day instead of an empty/zeroed read model.
- Re-implementing macro math in a new place instead of reusing `NutritionCalculator`.

## Suggested Implementation Order

1. Verify what `FoodItem`/`FoodCatalog` actually carry (key nutrients?) — decide if a catalog-extension slice is a prerequisite.
2. Meal-log domain + persistence (slice 1).
3. Meal-log commands + day consumption read model API (slice 2).
4. Water-intake log + hydration progress (slice 3).
5. Dashboard consumption query (slice 4, may fold into 2).

## Validation

Run backend build + tests (`./gradlew build`). Confirm: logging updates day totals; consumption compares against plan target reusing existing calculator; key nutrients null when unavailable; hydration sums; empty day returns zeroed read model, not 404.
