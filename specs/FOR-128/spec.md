# FOR-128 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-128
Epic: FOR-96 UI Backend Enablers — Foundations
Related: completes the documented FOR-127 gap; part of FOR-102 nutrition-consumption work.

## Summary

Resolve a calendar date to a `NutritionDayType` (RUNNING/STRENGTH/REST) and use it to
populate `target`/`comparison` in `GET /api/v1/nutrition/consumption`, which FOR-127
currently always returns as `null`. The nutrition domain has no date→day-type mapping
today — `NutritionDayTemplate` is keyed by `NutritionDayType`, not by date — so a date
cannot be resolved to a plan target. This story adds a pure derivation reusing the
existing deterministic weekly training-schedule policy (no new persistence), then wires
it into the consumption read model.

## User/System Flow

1. Frontend GETs `GET /api/v1/nutrition/consumption?date=YYYY-MM-DD`.
2. Backend resolves the date → `NutritionDayType` via the shared training-schedule day policy.
3. Backend looks up the `NutritionDayTemplate` for that type, computes the day's target macros, and compares consumed vs target.
4. Response now includes non-null `target` + `comparison` (previously always null).

## Functional Requirements

- A date→`NutritionDayType` resolver: running day → RUNNING, strength day → STRENGTH, otherwise REST.
- The day classification MUST be shared with `WeeklyTrainingScheduleService` (extract/share the existing policy), not duplicated.
- Wire the resolver into `GET /api/v1/nutrition/consumption`: look up the `NutritionDayTemplate` for the resolved type and populate `target` + `comparison`, reusing `NutritionCalculator`/`NutritionTotals`/`TargetComparison`.
- FOR-127's meal-logging and consumed-totals behavior must not regress.

## Non-Functional Requirements

- **No new persistence/migration** — pure derivation over the existing deterministic policy.
- **No duplicated math or policy**: reuse the training day-classification and the nutrition calculators.
- Owner-scoped per ADR-002 (consistent with FOR-127).

## Data Model Notes

- Existing: `WeeklyTrainingScheduleService` maps `DayOfWeek` → training via a documented MVP policy (Running Tue/Thu/Sat, Strength Mon PUSH / Wed PULL / Fri LEGS, Rest otherwise). Extract the day-kind classification so both the training schedule and this resolver use one source of truth.
- Existing: `NutritionDayType` (RUNNING/STRENGTH/REST), `NutritionDayCatalog`/`NutritionDayTemplate` keyed by type, `NutritionCalculator`, `NutritionTotals`, `TargetComparison`.
- No new tables, no migration (head remains V13).

## Edge Cases

- A running day AND a strength day mapping: the training policy already separates days (running Tue/Thu/Sat, strength Mon/Wed/Fri) so there is no same-day conflict under the MVP policy; if a future policy allows both, document the precedence (e.g. RUNNING wins) — for now assert the MVP mapping.
- Rest day (e.g. Sunday) → REST target.
- Empty day (no logs) → consumed zeroed, but `target`/`comparison` now populated from the resolved day type (this is the visible change vs FOR-127).
- A `NutritionDayType` with no template in the catalog → document behavior (should not happen for the closed enum, but fail safe: null target for that type, not a crash).

## Open Questions

- Should the shared day-classification live in the domain or stay an application-layer policy? Prefer wherever `WeeklyTrainingScheduleService`'s current logic naturally factors to; document.
- Same-day running+strength precedence — not reachable under the current MVP policy; document the assumption and revisit if the policy changes.
- Long term this MVP policy should be replaced by a real, user-configurable date schedule (out of scope here) — see FOR-102.
