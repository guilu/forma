# FOR-139 Test Plan

Strict TDD: failing tests first at each layer (streak rule → weekly-history buckets → API), then
implement. No migration.

## Scope

The streak and weekly-history read models and their read endpoints. Reuses existing per-date
history (nutrition/measurements) + training services; no persistence.

## Domain / Application Tests

- **Streak continuation**: consecutive consistent days (per the documented qualifying-activity rule) → `currentStreakDays` counts the run ending at `asOf`.
- **Gap reset**: a day with no qualifying activity resets the current streak to 0; a later run starts fresh.
- **Longest**: `longestStreakDays` = the longest consecutive run in the window, independent of the current run.
- **Today-inclusivity**: assert the documented rule (e.g. today active → counts; today inactive → per the chosen grace rule).
- **Empty history**: streak → `0`/`0`; not an error.
- **Weekly-history buckets**: bounded per-week series; each week bucketed correctly by `weekStart`; weeks with no data → zero bucket, still present.
- **Reuse**: assert derivation goes through `MealLogRepository`/`BodyMeasurementRepository` and existing training services (no duplicated schedule/policy logic).

## API Tests

- `GET /progress/streak` → `{ currentStreakDays, longestStreakDays, asOf }` per `api.md`.
- `GET /progress/weekly-history` → per-week series per `api.md`.
- Empty history → zeroed payloads (streak 0/0; weekly-history zero buckets), never 404.
- Any exposed window parameter out of range/non-numeric → 400 `VALIDATION_ERROR`.

## Edge Cases

- Single active day today → `currentStreakDays` = 1.
- Gap day inside a run → current resets, longest preserved.
- Window boundary (oldest partial week) → documented inclusivity holds.
- No linked history → zero/empty, not an error.

## Fixtures

- Per-date nutrition meal-log entries forming a run with a deliberate gap day; measurement dates for the optional qualifying signal.
- Multi-week history spanning the weekly-history window incl. an empty week.
- H2-in-PostgreSQL-mode with Flyway (no new migration; head stays V18) for the API path, matching FOR-129 style — or a `@WebMvcTest`/service-level slice with fakes where no persistence is touched.
