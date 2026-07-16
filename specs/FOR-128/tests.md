# FOR-128 Test Plan

Strict TDD: failing tests first (resolver → updated read model → API), then implement.

## Scope

The date→`NutritionDayType` resolver and its wiring into the consumption read model so `target`/`comparison` populate. FOR-127 logging behavior must not regress.

## Resolver Tests

- A Tuesday/Thursday/Saturday resolves to RUNNING (per the training policy).
- A Monday/Wednesday/Friday resolves to STRENGTH.
- Any remaining day (e.g. Sunday) resolves to REST.
- The resolver uses the SAME day-classification source as `WeeklyTrainingScheduleService` (assert they agree; ideally a shared unit, so a policy change updates both).

## Application Tests

- Consumption for a date on a STRENGTH day → target derived from the strength `NutritionDayTemplate` via existing calculators; comparison computed via `TargetComparison`.
- Consumption for a REST day → target from the rest template.
- Empty day (no logs) → consumed zeroed, target/comparison now populated from the resolved day type.
- Macro math is reused (no new formula); assert against the existing calculator output.

## API Tests

- `GET /nutrition/consumption?date=` on a known STRENGTH date → non-null `target` + `comparison` matching the strength template.
- Same for a RUNNING and a REST date.
- Empty day → 200 with zeroed consumed and populated target (the visible change vs FOR-127).
- FOR-127 regression: logging an entry still updates consumed; invalid input still 400.

## Edge Cases

- Rest day target correct.
- (If reachable) a day type with no catalog template → fail safe (null target, no crash) — document.
- Running+strength same-day precedence — not reachable under the MVP policy; assert the documented mapping.

## Fixtures

- Dates chosen to hit each day type (e.g. a Wednesday for STRENGTH, a Saturday for RUNNING, a Sunday for REST).
- The existing `NutritionDayCatalog`/templates as the target source.
- H2-in-PostgreSQL-mode with Flyway (no new migration; head stays V13) for the API/persistence integration path, matching FOR-127 test style.
