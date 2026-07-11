# FOR-100: Add muscle mass and body water to body measurement

Jira: https://dbhlab.atlassian.net/browse/FOR-100
Epic: FOR-95 UI Backend Enablers

## Summary

Extend the body-measurement domain + persistence + API with two optional fields
`muscleMassKg` and `waterPercentage` that FOR-15 explicitly deferred, so the
Mediciones UI (FOR-52) can show the "AGUA CORPORAL" card and the body-distribution
data. Backward compatible — the fields are nullable and existing measurements
still load.

## User/System Flow

1. Client `POST /api/v1/body/measurements` with the (optional) new fields.
2. They are persisted (FOR-16) and returned by `GET /api/v1/body/measurements`,
   carried as-is (no fake precision).
3. FOR-52 renders water % and (partial) body distribution.

## Functional Requirements

- Add `muscleMassKg` (Double, nullable) and `waterPercentage` (Double, nullable,
  `[0,100]`) to the `BodyMeasurement` domain record; validate ranges at
  construction like the existing fields (positive mass; percentage in `[0,100]`).
- Thread the fields through:
  - FOR-16 persistence (`JdbcBodyMeasurementRepository` + a DB migration adding
    the columns, following the FOR-83 migration approach; additive/backward
    compatible).
  - FOR-17 API: `CreateBodyMeasurementRequest` (optional inputs) and
    `BodyMeasurementResponse` (omit when null, per existing `@JsonInclude`).
- Values are optional everywhere; absent stays null (never fabricated).

## Non-Functional Requirements

- Backward compatible: old rows/payloads without the fields still work.
- No fake precision; carry values as provided.

## Data Model Notes

`domain/BodyMeasurement` currently: `measuredAt`, `source`, `weightKg`,
`bodyFatPercentage`, `bmi`, `notes`. Add `muscleMassKg`, `waterPercentage`.
Note this is a **measured** muscle mass field, distinct from the *derived*
`leanMassKg()` already computed from weight × body-fat. Persistence via the FOR-16
JDBC repository + a new migration.

## Edge Cases

- Neither field provided → measurement still valid; both null.
- `waterPercentage` out of `[0,100]` → construction/validation error (400).
- Old persisted rows (no columns populated) → load with nulls.

## Open Questions

- Bone mass (for the full Músculo/Grasa/Hueso/Agua silhouette) — out of scope
  unless trivially available; document as deferred.
- Whether `muscleMassKg` should replace the UI's current use of derived
  `leanMassKg` — recommend keeping both distinct and let the UI choose; document.
