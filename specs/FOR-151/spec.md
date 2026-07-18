# FOR-151 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-151
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheets **Dieta / Fuerza / Running** (weekly day layout).
Slice 3 of 7 — **first**: the whole calendar, nutrition and insights derive from the days.

## Summary

Fix the weekly training-day mapping so it matches Diego's real plan. Today the days are effectively
inverted vs the Excel. Running days must stay derived from a single source (`RunningPlanGenerator`),
not a second hardcoded literal set.

## Day mapping (Excel vs current app, verified)

| | Diego (Excel) | App today |
|---|---|---|
| Running | **Lun / Mié / Sáb** | Mar / Jue / Sáb |
| Fuerza | **Mar (Empuje) / Jue (Tirón) / Dom (Pierna-Core)** | Lun (PUSH) / Mié (PULL) / Vie (LEGS) |
| Descanso | **Viernes** | Domingo |

## Current repository state (verified)

- `domain/WeeklyTrainingDayPolicy.java:33-38` — `STRENGTH_DAYS = {MONDAY→PUSH, WEDNESDAY→PULL, FRIDAY→LEGS}`.
- `domain/RunningPlanGenerator.java:48-61` — each week schedules sessions on **TUESDAY / THURSDAY / SATURDAY**; `WeeklyTrainingDayPolicy.RUNNING_DAYS` is **derived** from `RunningPlanGenerator.sixteenWeekPlan()` (single source, not a second literal set).
- `domain/NutritionDayTypeResolver.java` — reuses `WeeklyTrainingDayPolicy.classify(...)`; will follow automatically once the policy changes.
- `WeeklyTrainingScheduleService` (application) builds the training calendar and stable session ids `<DAY>:<KIND>` from this policy; the FOR-136 muscle-map session ids ride on the same scheme.
- No migration involved — this is in-code policy/generator data.

## Functional Requirements

- `WeeklyTrainingDayPolicy.STRENGTH_DAYS` → `{TUESDAY→PUSH (Empuje), THURSDAY→PULL (Tirón), SUNDAY→LEGS (Pierna/Core)}`.
- Running days → **Monday / Wednesday / Saturday**, changed at the single source: the session days inside `RunningPlanGenerator.sixteenWeekPlan()` (so `RUNNING_DAYS` follows). Do **not** add a second literal running-day set.
- Rest day becomes **Friday** (any day not running/strength → REST; Friday is the only leftover).
- Verify `NutritionDayTypeResolver` stays coherent (running/strength/rest resolve to the corrected days) — it should require no change since it delegates to the policy.
- Keep `WorkoutType` PUSH/PULL/LEGS (concepts = Empuje/Tirón/Pierna-Core); only the day→type mapping changes. Exercise detail is slice 6 (FOR-154).

## Non-Functional Requirements

- Single source of truth preserved (ADR-001): running days derive from `RunningPlanGenerator`; the policy is shared by training calendar + nutrition resolver.
- Pure, deterministic, framework-free domain (ADR-001). Owner-scoped consumers unchanged (ADR-002).
- No migration (ADR-003 head stays V19).

## Data Model Notes

- Running days and strength days remain **disjoint** under the new mapping (Mon/Wed/Sat vs Tue/Thu/Sun), so `classify()`'s "running-checked-first" precedence stays a documented, unreached tie-breaker.
- Session ids change with the days (e.g. strength now on Tue/Thu/Sun); downstream consumers that key off `<DAY>:<KIND>` (FOR-136 muscle-map, schedule read model) shift accordingly.

## Edge Cases

- Friday now classifies as REST (was Sunday); Sunday now classifies as STRENGTH (Pierna/Core).
- Nutrition day-type for each weekday must match the corrected training day after the change.
- Any test asserting the old Mon/Wed/Fri or Tue/Thu/Sat literals must be updated to the new mapping.

## Open Questions

- None on the mapping itself (Excel is explicit). Confirm no other module hardcodes the old day literals beyond the policy + generator (grep before finishing).
