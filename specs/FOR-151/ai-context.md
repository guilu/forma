# FOR-151 AI Context

## Story

FOR-151 — Calendario de entreno: días reales de Diego. Slice 3 of epic FOR-148, sequenced **first**
(calendar, nutrition day-type and insights all derive from the days).

## Intent

Correct the weekly day mapping to Diego's real plan (running Lun/Mié/Sáb, strength Mar/Jue/Dom, rest
Vie) while keeping the single-source-of-truth design. Success = the training calendar, nutrition
resolver and session ids all reflect the corrected days.

## Relevant Documents

- `AGENTS.md` — hexagonal, single source of truth, no duplicated policy.
- `docs/fitness_os.xlsm` — sheets **Dieta / Fuerza / Running** (the weekly day layout). Source of truth.
- `docs/adr/ADR-001-architecture.md` (domain-owned policy, no duplication), `ADR-002-authentication.md` (owner-scoping), `ADR-003-persistence.md` (no migration needed).
- Downstream slices: `specs/FOR-153/` (running plan, keeps deriving days), `specs/FOR-154/` (strength exercise detail), `specs/FOR-136/` (muscle-map session ids).
- Jira: https://dbhlab.atlassian.net/browse/FOR-151

## Domain / Repo Notes (verified)

- `WeeklyTrainingDayPolicy` is the single policy shared by `WeeklyTrainingScheduleService` (training calendar) and `NutritionDayTypeResolver` (nutrition target).
- `RUNNING_DAYS` is derived from `RunningPlanGenerator.sixteenWeekPlan()` day-of-week values — change the generator's session days, not a separate literal.
- `STRENGTH_DAYS` is a literal `EnumMap` (Mon/Wed/Fri today) — this is the map to edit.
- `classify()` checks running first, then strength, else REST; the sets are disjoint so precedence is documented-but-unreached.

## Architectural Constraints

- Do NOT introduce a second running-day literal set — running days must keep deriving from `RunningPlanGenerator`.
- Pure, deterministic, framework-free (ADR-001). No migration (ADR-003; head stays V19).
- Keep `WorkoutType` PUSH/PULL/LEGS; only remap days to types (Mar→PUSH, Jue→PULL, Dom→LEGS).

## Common Pitfalls

- Hardcoding new running days in `WeeklyTrainingDayPolicy` instead of `RunningPlanGenerator` (breaks single source).
- Renaming/replacing `WorkoutType` enum values (Empuje/Tirón/Pierna-Core are the same concepts as PUSH/PULL/LEGS — keep the enum).
- Forgetting Friday→REST and Sunday→STRENGTH flips in nutrition/insights day-type tests.
- Missing a module that hardcodes the old day literals — grep the codebase before finishing.
- Editing exercise content here — that is slice 6 (FOR-154).

## Suggested Implementation Order

1. Change `RunningPlanGenerator` session days to Monday/Wednesday/Saturday (single source).
2. Remap `WeeklyTrainingDayPolicy.STRENGTH_DAYS` to Tuesday→PUSH, Thursday→PULL, Sunday→LEGS.
3. Verify `NutritionDayTypeResolver` + schedule read model reflect the corrected days (should need no change).
4. Update tests asserting the old day literals; grep for other hardcoded day references.

## Validation

Backend build + tests (`./gradlew build`). Confirm: running days Mon/Wed/Sat via the generator; strength Tue/Thu/Sun; Friday REST; nutrition day-type matches per weekday; single source preserved (no duplicated running-day literals); no migration (head V19); session ids reflect the new days.
