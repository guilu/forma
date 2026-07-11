# FOR-100 AI Context

## Story

FOR-100 — Add muscle mass and body water to body measurement
(https://dbhlab.atlassian.net/browse/FOR-100)

## Intent

Back the FOR-52 water card + body-distribution data by adding the two fields
FOR-15 deferred. Success is `muscleMassKg` + `waterPercentage` flowing through
domain → persistence → API, optional and backward compatible.

## Relevant Documents

- `AGENTS.md`
- `docs/api/body-measurements.md`, `docs/api-conventions.md`
- `docs/adr/ADR-005-api-design.md`, `docs/adr/ADR-001-architecture.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/` (domain, deferred these), `specs/FOR-16/` (persistence),
  `specs/FOR-17/` (API), `specs/FOR-83/` (migrations)
- Jira: https://dbhlab.atlassian.net/browse/FOR-100

## Domain Notes

- `domain/BodyMeasurement` validates at construction (positive weight, body-fat in
  `[0,100]`). Add the two fields with the same style; percentage in `[0,100]`,
  mass strictly positive when present.
- `leanMassKg()` is DERIVED (weight × body-fat) — keep it; `muscleMassKg` is a new
  MEASURED field, do not conflate.
- Persistence: `adapter/persistence/JdbcBodyMeasurementRepository` + a migration
  (FOR-83 approach). API DTOs: `CreateBodyMeasurementRequest`,
  `BodyMeasurementResponse` (`@JsonInclude(NON_NULL)`).

## Architectural Constraints

- Domain framework-free (ADR-001). Migration additive/backward compatible. DTOs
  distinct from domain (ADR-005). Optional/nullable throughout.

## Common Pitfalls

- Making the fields required (breaks existing rows/payloads).
- Conflating measured `muscleMassKg` with derived `leanMassKg`.
- Forgetting the DB migration or the persistence read/write mapping.

## Suggested Implementation Order

1. Add fields + validation to `BodyMeasurement` (+ domain tests).
2. Migration adding the columns; update the JDBC repository read/write.
3. Extend `CreateBodyMeasurementRequest` + `BodyMeasurementResponse`.
4. Round-trip test (create with fields → read back); back-compat test (nulls).

## Validation

Run `./gradlew test spotlessApply` from `backend/`; run migrations against the
local DB. Verify create/read round-trips the new fields and old rows still load.
