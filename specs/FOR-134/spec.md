# FOR-134 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-134
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-102 [STUB] Nutrition consumption logging + hydration (key-nutrients slice). Extends FOR-127.

## Summary

Extend the reference food data with **fiber, sugars, sodium and saturated fat**, and
surface consumed key-nutrient totals in the day consumption read model
(`GET /api/v1/nutrition/consumption`, from FOR-127). Reference data only — no external
nutrition database. Handles the prerequisite FOR-102 flagged: `FoodItem` carries none of
these today.

## Repository baseline (verified)

- `FoodItem` carries only `kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`, `defaultServingG` — NO key nutrients.
- The catalog is **in-code**: `FoodCatalog` holds 12 hardcoded `FoodItem`s. There is NO food DB table/migration → extending the reference data needs **no migration** (head stays V16).
- FOR-127 meal logging + `GET /nutrition/consumption` computes consumed macros via `NutritionCalculator`.

## Functional Requirements

- Extend `FoodItem` with per-100 g **nullable** fields: `fiberPer100g`, `sugarsPer100g`, `sodiumMgPer100g`, `saturatedFatPer100g` (Double; nullable because reference data may be incomplete). Non-negative when present.
- Populate the 12 `FoodCatalog` entries with known values; leave null where unknown — **never fabricate**.
- Extend consumed-totals computation (reuse/extend `NutritionCalculator`/`NutritionTotals`) to sum consumed key nutrients from logged FoodItems (FOR-127 meal logs), scaled by grams like macros.
- Surface consumed key-nutrient totals in `GET /api/v1/nutrition/consumption`.
- Free/ad-hoc meal entries (FOR-127) may optionally provide key nutrients; else null.

## Non-Functional Requirements

- **No migration** — in-code reference data; head stays V16.
- **No duplicated math** — extend the existing calculator; nullable nutrients propagate honestly.
- No regression to FOR-127 macro consumption.
- Owner-scoped per ADR-002.

## Data Model Notes

- `FoodItem` gains 4 nullable per-100 g fields. `sodiumMgPer100g` in milligrams (sodium is conventionally mg); the other three in grams. Document units.
- `NutritionTotals` (or the consumption read model) gains consumed fiber/sugars/sodium/saturatedFat totals — nullable.
- Free-entry key nutrients: optional inputs on the meal-log entry, else null.

## Null / partial-total rule (decide + document)

When summing a day's consumed key nutrient and one or more contributing foods lack that nutrient:
- Option A: the total is **null** (can't be complete) — honest but coarse.
- Option B: total the foods that HAVE the value and flag it partial.
Pick one, document it, and keep it consistent across the four nutrients. Recommended for MVP: partial total over the foods that carry the value, with the per-food null simply contributing 0 to that nutrient — but ONLY if that is clearly documented so a partial total isn't mistaken for complete. Implementer decides and documents.

## Edge Cases

- Food with no key-nutrient data → contributes null (per the chosen rule); never fabricated.
- Day with no logs → consumption key-nutrient totals zero/null (per rule), never 404 (unchanged from FOR-127).
- Free entry without key nutrients → null.
- Negative key-nutrient value → 400 `VALIDATION_ERROR` (validation at construction).

## Open Questions

- Null-vs-partial total rule (above) — pick + document.
- Units: sodium in mg, others in g — confirm + document; ensure the API response labels them unambiguously.
- Whether to also expose key nutrients per logged entry, or only the day total — default day total for MVP; document.
