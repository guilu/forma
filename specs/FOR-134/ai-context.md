# FOR-134 AI Context

## Story

FOR-134 — Key nutrients (fiber, sugars, sodium, saturated fat) in food catalog and day consumption. Key-nutrients slice of FOR-102 [STUB] Nutrition consumption logging + hydration. Extends FOR-127's consumption read model.

## Intent

Add key-nutrient tracking (fibra/azúcares/sodio/grasas saturadas) on top of FOR-127's macro consumption. Success = `GET /api/v1/nutrition/consumption` reports consumed key-nutrient totals, and FOR-54 can surface them. The prerequisite (FoodItem carries none) is handled here.

## Relevant Documents

- `specs/FOR-102/` (full scope; flagged this needs a FoodItem extension), `specs/FOR-127/` (the consumption slice this extends).
- `AGENTS.md` — hexagonal, no duplicated math, never fabricate data.
- `docs/adr/ADR-001-architecture.md`, `ADR-005-api-design.md`, `docs/domain-model.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-134

## Domain / Repo Notes (verified)

- `FoodItem` = `id,name,kcalPer100g,proteinPer100g,carbsPer100g,fatPer100g,defaultServingG` — NO key nutrients. Validates non-negative at construction.
- The catalog is **in-code**: `FoodCatalog` has 12 hardcoded `new FoodItem(...)`. NO food DB migration exists → extending reference data needs **no migration** (head stays V16).
- FOR-127: `MealLogService`/consumption read model computes consumed macros via `NutritionCalculator`; `delivery/nutrition` `DayConsumptionResponse`; free entries carry provided macros.
- FOR-128 added `dayType` + target/comparison to the same consumption response — do not disturb.

## Architectural Constraints

- Extend `FoodItem` with 4 **nullable** per-100 g fields (fiber g, sugars g, sodium mg, saturated fat g); non-negative when present. Nullable because reference data is incomplete — never fabricate.
- Populate the 12 catalog entries with known values, null where unknown.
- Extend `NutritionCalculator`/`NutritionTotals` for consumed key nutrients — do NOT duplicate macro math; macros must not regress.
- No migration (in-code catalog). Owner-scoped per ADR-002.
- Free-entry key nutrients optional (extend the FOR-127 log request DTO); else null.

## Common Pitfalls

- Making the new FoodItem fields non-null primitives — they MUST be nullable; incomplete reference data is normal.
- Fabricating key-nutrient values for foods that don't have real data.
- Duplicating macro/scaling math instead of extending `NutritionCalculator`.
- Regressing FOR-127 macro consumption or FOR-128 target/dayType.
- Adding a migration for what is in-code reference data.
- Mixing sodium units (it is mg, not g) — document and keep consistent.

## Suggested Implementation Order

1. Extend `FoodItem` with nullable fiber/sugars/sodium(mg)/saturatedFat (+ validation tests).
2. Populate the 12 `FoodCatalog` entries with known values / null (+ a couple of known-null assertions).
3. Extend the calculator + consumption read model for consumed key-nutrient totals, applying the documented null/partial-total rule (+ tests).
4. Extend `GET /nutrition/consumption` response + the free-entry `POST /nutrition/log` request DTO (+ API tests). Confirm FOR-127/128 regression tests pass.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: FoodItem accepts nullable key nutrients (non-negative when present); catalog entries expose known values / null (no fabrication); consumption returns consumed key-nutrient totals with correct units (sodium mg, others g) per the documented null/partial rule; macros + target/dayType unchanged; free-entry key nutrients optional; no migration added (head V16).
