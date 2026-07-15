# FOR-127 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-127
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-102 [STUB] Nutrition consumption logging + hydration (slice 1).

## Summary

First implementable slice of FOR-102: **meal consumption logging + day consumption vs
target**. Persist what the user actually ate and expose a "consumed vs plan target"
read model over HTTP, reusing the existing plan-side calculators. Macros only (kcal,
protein, carbs, fat). Hydration and key-nutrient tracking (fibra/azúcares/sodio/grasas
saturadas) are later FOR-102 slices. Today the nutrition domain is plan-only
(`NutritionDay`, `NutritionTotals`, `TargetComparison`, `FoodItem`, `MealTemplate`) —
there is no record of real consumption.

## User/System Flow

1. User opens Nutrición (FOR-54) and taps "Registrar comida" → `POST /api/v1/nutrition/log`.
2. Backend persists the logged entry and recomputes the day's consumed totals.
3. Frontend GETs `GET /api/v1/nutrition/consumption?date=` → consumed macros vs plan target.
4. Dashboard reads the same read model for "calorías consumidas vs objetivo".

## Functional Requirements

- Persist a `MealLogEntry`: day, meal type, and either a catalog `FoodItem` reference + portions, or a free/ad-hoc entry with provided macros.
- A per-day meal-log aggregate accumulates the day's logged entries.
- `POST /api/v1/nutrition/log` — add an entry.
- `GET /api/v1/nutrition/consumption?date=YYYY-MM-DD` — consumed macros (kcal, protein, carbs, fat) vs the plan target, with a comparison.
- Macro math reuses `NutritionCalculator`/`NutritionTotals`; comparison reuses `TargetComparison`. No duplicated math.
- Owner-scoped (single-user MVP).

## Non-Functional Requirements

- **Security/Privacy**: consumed food is personal health data — never log entry contents at INFO; owner-scoped per ADR-002; do not bypass the owner boundary.
- **Performance**: per-day, low volume; a single query per day is fine.
- **Correctness**: logging is additive and MUST NOT mutate plan templates.

## Data Model Notes

- New domain: `MealLogEntry` + a per-day meal-log aggregate. Implementer chooses aggregate shape (per-day vs per-entry rows); document.
- Reuse existing: `FoodItem`, `FoodCatalog`, `MealType`, `NutritionTotals`, `NutritionCalculator`, `TargetComparison`, `NutritionDay(Template)` (for the target side).
- New migration: next free version is **V13** (current head V12 after FOR-126); one column per statement (H2/PostgreSQL convention).
- Consumed totals are derived on read (or maintained on write) — do NOT store a duplicated denormalized copy that can drift from the entries; document the choice.

## Edge Cases

- Day with no plan target → return consumed totals with null/omitted comparison, not an error.
- Free/ad-hoc entry with no catalog food → store provided macros; entry still counts toward totals.
- Empty day (no logs) → `GET` returns 200 with zeroed consumption, never 404.
- `POST` with neither `foodItemId` nor macros, negative portions, or unknown `mealType` → 400 `VALIDATION_ERROR`.
- Multiple entries same meal/day → all counted; never overwrite.

## Open Questions

- Aggregate shape (per-day aggregate vs per-entry rows summed on read) — pick the simplest consistent with FOR-107/125 persistence style; document.
- Is edit/delete of a logged entry in this slice, or append-only for MVP? Default append-only unless trivial; document.
- Consumed totals: derived-on-read vs maintained-on-write — document the choice and guarantee they never drift from the entries.
- Key nutrients (fibra/azúcares/sodio/grasas saturadas) are OUT of this slice; a later slice must first confirm whether `FoodItem` carries them (possible catalog-extension prerequisite — see specs/FOR-102).
