# FOR-102 Spec

> ⚠️ **Epic-sized stub.** This folder captures the full scope of the `[STUB]` story
> and proposes an implementation slicing. It does NOT create new Jira issues. Before
> implementing, split into the child stories proposed under *Proposed story slices*
> and implement one slice per PR with `jira-sdd-ai`.

Jira: https://dbhlab.atlassian.net/browse/FOR-102
Epic: FOR-96 UI Backend Enablers — Foundations

## Summary

Persist what the user actually consumed (logged meals vs the plan) and water intake,
and expose "consumed vs target" and hydration progress read models over HTTP. Today
the nutrition domain is **plan-only** (`NutritionDay`, `NutritionDayTemplate`,
`NutritionTotals`, `TargetComparison`, `FoodItem`, `MealTemplate`) — there is no
record of real consumption and no hydration concept anywhere in the backend.

## User/System Flow

1. User opens Nutrición (FOR-54) and taps "Registrar comida" → frontend POSTs a meal-log entry (food/portion or free items) for a given day.
2. Backend persists the logged meal and recomputes the day's consumed totals.
3. User taps "Añadir agua" → frontend POSTs a hydration entry (volume) for the day.
4. Frontend GETs the day's consumption read model: consumed vs target macros + key nutrients, and hydration progress vs goal.
5. Dashboard GETs "calories eaten vs target" from the same read model.

## Functional Requirements

- Persist a **meal log**: consumed portions per meal/day, referencing `FoodItem` from `FoodCatalog` where possible, plus free/ad-hoc entries.
- Persist a **water-intake log**: hydration entries (volume + timestamp) per day.
- Compute a **day consumption read model**: consumed macros (kcal, protein, carbs, fat) vs the plan target, reusing `NutritionCalculator`/`NutritionTotals`/`TargetComparison` rather than duplicating math.
- Track **key nutrients** where available: fibra, azúcares, sodio, grasas saturadas — expose only what `FoodItem` genuinely carries; if the catalog lacks a nutrient, expose null, never fabricate.
- Compute a **hydration progress read model**: total logged volume vs a daily hydration goal (goal source: `DefaultObjectives.dailyWater` from FOR-107, else a documented default).
- Expose logging (commands) and reading (queries) endpoints — see `api.md`.

## Non-Functional Requirements

- **Performance**: day read models are per-day, low volume; a single query per day is acceptable at MVP.
- **Security/Privacy**: consumed food and hydration are personal health data — never log entry contents at INFO; owner-scoped per ADR-002 (single-user MVP, fixed owner constant, do not bypass the owner boundary).
- **Observability**: log-and-read operations must be traceable without leaking nutrient/volume values.

## Data Model Notes

- New domain: `MealLogEntry` (day, meal type, food ref or free item, portion) and `DayMealLog` aggregate, or a per-day log aggregate — implementer's choice, documented.
- New domain: `WaterIntakeEntry` + per-day hydration aggregate.
- Reuse existing: `FoodItem`, `MealType`, `NutritionTotals`, `NutritionCalculator`, `TargetComparison`, `NutritionDay(Template)` for the target side.
- New migration(s): next free version is **V11+** (current head is V10). One column per `ALTER`/`CREATE` statement per the H2/PostgreSQL convention already used (V6/V7/V9 lesson).
- Consumed-vs-target must NOT mutate the plan; logging is additive and separate from the plan templates.

## Edge Cases

- Logging for a day with no plan target → return consumed totals with null/omitted comparison, not an error.
- Free/ad-hoc item with no catalog food → store its provided macros; key nutrients null if not provided.
- Multiple hydration entries same day → sum; never overwrite.
- Editing/deleting a logged entry recomputes the day totals (if edit/delete is in scope of the slice).
- Day with logs but no hydration goal available → expose total volume with null goal/progress.

## Proposed story slices

1. **Meal-log domain + persistence** — `MealLogEntry`/day aggregate, repository, migration; no HTTP yet.
2. **Meal-log commands + day consumption read model API** — POST log entry, GET day consumption vs target (+ key nutrients).
3. **Water-intake log domain + persistence + API** — hydration entries + hydration progress read model.
4. **Dashboard consumption read model** — "calories eaten vs target" widget query (may fold into slice 2).

## Open Questions

- Does the food catalog (`FoodItem`) actually carry fibra/azúcares/sodio/grasas saturadas today? Verify against `FoodItem`/`FoodCatalog`; if not, a prerequisite slice must extend the catalog before key-nutrient tracking is possible.
- Is edit/delete of a logged meal in scope, or append-only for MVP?
- Hydration goal source and default when `DefaultObjectives.dailyWater` is unset.
