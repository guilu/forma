# FOR-130 Test Plan

Strict TDD: failing tests first at each layer (domain → application → persistence → API), then implement.

## Scope

Water-intake logging + hydration progress read model. Goal resolved from the FOR-107 profile with a fallback default. Meal logging / consumed-vs-target are out of scope (done in FOR-127/128).

## Domain Tests

- Hydration aggregate: multiple entries in a day sum; never overwrite.
- `WaterIntakeEntry` rejects non-positive volume.
- Progress = total/goal; goal null → progress null; documented cap behavior on total>goal.

## Application Tests

- Logging a volume → hydration read model reflects the new total.
- Goal read from `DefaultObjectives.dailyWaterMl` via the profile; fallback default applied when null (documented value); progress null only when goal cannot be determined.
- Owner-scoping: only the owner's entries count.
- Reuses the profile service for the goal (no duplicated profile logic).

## Persistence Tests

- Round-trip a day's water entries through the JDBC adapter against H2-in-PostgreSQL-mode with Flyway (V14).
- Empty DB / empty day → total 0, no error.

## API Tests

- `POST /nutrition/hydration` valid → 200/201; subsequent GET reflects the total.
- `POST /nutrition/hydration` volume <= 0 → 400; invalid/far-future date → 400.
- `GET /nutrition/hydration` before any log → 200 total 0 (goal/progress resolved), never 404.
- `GET /nutrition/hydration` with a profile goal set → correct goal + progress; with goal unset → fallback default (or null progress) per the documented decision.
- Multiple POSTs same day → total sums.

## Edge Cases

- Goal unset → documented fallback / null progress.
- total > goal → documented cap or raw.
- Far-future date → 400.

## Fixtures

- A profile with `dailyWaterMl` set and one without (EMPTY) to exercise goal vs fallback.
- Multiple same-day entries to exercise summing.
- H2-in-PostgreSQL-mode with Flyway migrations (V14) for persistence/API integration, matching FOR-127 test style.
