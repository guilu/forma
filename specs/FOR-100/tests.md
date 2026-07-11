# FOR-100 Test Plan

## Scope

Verify `muscleMassKg` + `waterPercentage` flow through domain, persistence and
API, optional and backward compatible.

## Domain Tests

- A `BodyMeasurement` accepts both new fields and exposes them.
- Both absent → measurement still valid (fields null).
- `waterPercentage` outside `[0,100]` → rejected at construction.
- Measured `muscleMassKg` is independent of derived `leanMassKg()`.

## Application Tests

- Create + read through the service preserves the new fields.

## API Tests

- `POST /api/v1/body/measurements` with the new fields persists and returns them.
- `POST` without them succeeds; response omits the null fields (`@JsonInclude`).
- `GET` returns the fields for measurements that have them.
- Out-of-range `waterPercentage` → 400 `VALIDATION_ERROR`.

## UI Tests

N/A — backend story (UI consumption is FOR-52 / a follow-up).

## Edge Cases

- Old persisted rows (columns null) load without error.
- Migration is additive/backward compatible.

## Fixtures

- Measurements with both fields, with neither, and an out-of-range water value.
