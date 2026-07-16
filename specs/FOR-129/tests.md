# FOR-129 Test Plan

Strict TDD: failing tests first (read model → application → API), then implement.

## Scope

The adherence read model: per-category planned vs completed over a rolling window, derived from existing training/nutrition/measurement data. No persistence added; other FOR-104 slices out of scope.

## Application Tests

- TRAINING: planned counted from the schedule over the window; completed counted from COMPLETED session statuses; reuses the shared policy/services (assert against their output, no re-derivation).
- NUTRITION: completed = days-with-log (FOR-127) in the window; planned = days in window; documented definition honored.
- MEASUREMENTS: completed = actual BodyMeasurement entries in the window; planned = expected from the assumed cadence.
- `rate` = completed/planned; planned 0 → rate null (or 0 per chosen convention), no exception.
- Window bounds honored (`[today-days+1, today]` or documented equivalent).
- Owner-scoping: only the owner's data counts.

## API Tests

- `GET /progress/adherence` default (no days) → 30-day window; response shape per `api.md`.
- `GET /progress/adherence?days=7` → 7-day window computed.
- `days` out of range / non-numeric → 400 `VALIDATION_ERROR`.
- Empty data (no sessions/logs/measurements) → 200 with zeroed categories, never 404.
- Each category's planned/completed match hand-computed fixtures.

## Edge Cases

- Zero planned training in window → planned 0, rate null/0.
- `completed > planned` (extra measurements) → documented behavior (cap at 1.0 or raw).
- Window of 1 day.

## Fixtures

- Training sessions across the window with a mix of COMPLETED/PLANNED/SKIPPED.
- Meal logs on some days but not others (to exercise days-with-log).
- BodyMeasurements at a cadence above and below the expected one.
- H2-in-PostgreSQL-mode with Flyway (no new migration; head stays V13) for the API integration path, matching FOR-127/128 test style.
