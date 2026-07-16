# FOR-128 AI Context

## Story

FOR-128 — Resolve nutrition day type by date and populate consumption target/comparison. Completes the documented FOR-127 gap. Part of FOR-102 nutrition-consumption work.

## Intent

FOR-127 shipped meal logging + consumed macro totals, but `target`/`comparison` in `GET /api/v1/nutrition/consumption` are always null because nothing maps a calendar date to a `NutritionDayType`. This story adds that mapping (reusing the existing training-schedule day policy, no new persistence) and wires it into the consumption read model so consumed-vs-target works. Success = the endpoint (and the dashboard "calorías consumidas vs objetivo" widget, FOR-54) can show target + comparison.

## Relevant Documents

- `specs/FOR-127/` — the meal-logging slice this completes.
- `specs/FOR-102/` — full nutrition-consumption scope.
- `AGENTS.md` — hexagonal boundaries, owner-scoping, no duplicated logic.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-005-api-design.md`, `docs/domain-model.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-128

## Domain Notes

- **Root gap** (verified): `NutritionDayTemplate` is keyed by `NutritionDayType` (RUNNING/STRENGTH/REST), not by date. No date→day-type mapping exists → FOR-127 returns null target.
- **Reuse**: `WeeklyTrainingScheduleService` already classifies each `DayOfWeek` by a documented deterministic MVP policy — Running Tue/Thu/Sat, Strength Mon (PUSH) / Wed (PULL) / Fri (LEGS), Rest otherwise. Extract/share that day classification so the nutrition resolver and the training schedule stay a single source of truth.
- **Reuse for target**: `NutritionDayCatalog`/`NutritionDayTemplate`, `NutritionCalculator`, `NutritionTotals`, `TargetComparison` — do NOT duplicate macro math (FOR-105/FOR-127 already use these).
- No new table/migration — head stays V13.

## Architectural Constraints

- Pure derivation; hexagonal boundaries. The shared day-classification should factor to wherever `WeeklyTrainingScheduleService`'s logic naturally lives (application-layer policy is fine); avoid a circular dependency between nutrition and training services — prefer a small shared policy/helper both depend on.
- Owner-scoped per ADR-002 (consistent with FOR-127).
- Modify only the consumption read model + add the resolver; do NOT change FOR-127 logging.

## Common Pitfalls

- Duplicating the training day policy instead of sharing it (the two would drift).
- Duplicating macro math instead of reusing `NutritionCalculator`/`TargetComparison`.
- Adding a persistence table/migration for what is a pure deterministic derivation this slice.
- Regressing FOR-127 logging / consumed-totals behavior.
- Introducing a nutrition↔training service cycle — extract a shared policy instead.

## Suggested Implementation Order

1. Extract/share the `DayOfWeek`→day-kind classification currently inside `WeeklyTrainingScheduleService`; add tests that both consumers agree.
2. Add the date→`NutritionDayType` resolver on top of it (+ tests).
3. Wire the resolver into the consumption service: resolve day type → look up `NutritionDayTemplate` → compute target + comparison via existing calculators (+ application tests).
4. Update `delivery/nutrition` consumption response so `target`/`comparison` (and optionally `dayType`) populate (+ API tests). Confirm FOR-127 regression tests still pass.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: a date resolves to the correct NutritionDayType per the shared policy; consumption returns non-null target + comparison derived from the day type's template; empty day is consumed-zeroed with populated target; FOR-127 logging unaffected; no new migration.
