# FOR-151 Test Plan

Strict TDD: update/author failing tests for the new day mapping first, then change the policy/generator.

## Scope

The corrected weekly day mapping across the shared policy, the running generator's session days, the
nutrition day-type resolver, and the schedule read model / session ids.

## Domain Tests

- `WeeklyTrainingDayPolicy.strengthDays()` = {TUESDAY→PUSH, THURSDAY→PULL, SUNDAY→LEGS}.
- `WeeklyTrainingDayPolicy.runningDays()` = {MONDAY, WEDNESDAY, SATURDAY}, derived from `RunningPlanGenerator`.
- `classify(day)`: Mon/Wed/Sat → RUNNING; Tue/Thu/Sun → STRENGTH; Friday → REST.
- `RunningPlanGenerator.sixteenWeekPlan()` schedules each week's 3 sessions on Mon/Wed/Sat.

## Application Tests

- `NutritionDayTypeResolver.resolve(date)` matches the corrected day for each weekday (no separate change needed — assert it follows the policy).
- `WeeklyTrainingScheduleService` produces session ids on the new days.

## API Tests

- `GET /api/v1/training/schedule` shows strength Tue/Thu/Sun, running Mon/Wed/Sat, rest Fri.
- FOR-136 muscle-map resolves a new strength session id (e.g. `TUESDAY:STRENGTH`).

## Edge Cases

- Friday classifies as REST (was Sunday).
- Sunday classifies as STRENGTH/LEGS (was REST).
- Running vs strength sets stay disjoint (precedence tie-breaker unreached).

## Fixtures

- The 7 weekdays as the exhaustive classification fixture.
- Existing running-plan / schedule tests updated from Tue/Thu/Sat and Mon/Wed/Fri to the new mapping.
