# FOR-16: Persist body measurements

Jira: https://dbhlab.atlassian.net/browse/FOR-16
Epic: FOR-2 Body Composition

## Summary

Add persistence for `BodyMeasurement` (FOR-15): a Flyway migration, a
repository port + JDBC adapter, persistence mapping, and a basic integration
test. PostgreSQL-compatible schema with adequate numeric precision; list
results order by `measuredAt` descending by default.

## User/System Flow

1. An application use case (introduced by this or the FOR-17 API story) calls
   the repository to save a manually entered measurement.
2. The JDBC adapter maps the domain object to a row and inserts it.
3. The repository lists persisted measurements ordered by `measured_at`
   descending, ready for FOR-17 (API) and FOR-19/FOR-20 (dashboard/graphs) to
   consume.

## Functional Requirements

- Add a new Flyway migration after the existing baseline
  (`backend/src/main/resources/db/migration/V1__baseline.sql`, currently
  empty) creating a `body_measurements` table. Verify the next free version
  number in the repo before naming the file (e.g. `V2__...`).
- Table columns cover `id`, `measured_at`, `source`, `weight_kg`,
  `body_fat_percentage`, `bmi`, `notes`. See Open Questions for
  `fat_mass_kg`/`lean_mass_kg`.
- Implement a repository port (application/domain boundary) and a JDBC-based
  adapter under `backend/src/main/java/dev/diegobarrioh/forma/adapter/` —
  the project uses `spring-boot-starter-jdbc` + Flyway, not JPA
  (`backend/build.gradle`), so the adapter is a plain JDBC mapper, not an ORM
  repository.
- `save(measurement)`: persists one `BodyMeasurement`.
- `list()`: returns measurements ordered by `measured_at` descending by
  default.
- No provider-specific columns (Withings tokens, sync metadata) in this
  table (docs/domain-model.md: keep provider data out of Body).

## Non-Functional Requirements

- Use `NUMERIC`/`DECIMAL` column types for `weight_kg`, `body_fat_percentage`
  and `bmi` to avoid floating-point precision loss.
- Migrations are additive; do not alter or drop the existing baseline
  migration (ADR-003).
- Integration test runs against the H2 PostgreSQL-compatibility mode already
  configured for backend tests (`backend/build.gradle` `testRuntimeOnly
  com.h2database:h2`), following the existing pattern in
  `backend/src/test/java/dev/diegobarrioh/forma/MigrationBaselineTest.java`.

## Data Model Notes

Mirrors docs/domain-model.md's `BodyMeasurement`, restricted to the fields in
scope for FOR-15 (`measuredAt`, `source`, `weightKg`, `bodyFatPercentage`,
`bmi`, `notes`) plus the derived `fatMassKg`/`leanMassKg`. `source` should be
stored so a later external source (e.g. `WITHINGS`) fits without a schema
rewrite (a short text/enum-like column, not a boolean "is manual" flag).

## Edge Cases

- Migration applied to a fresh database vs. a database that already has the
  `V1__baseline` migration applied.
- List ordering when multiple measurements share the same `measured_at`.
- Persisting/reading derived values consistently with the FOR-15 domain
  calculation (see Open Questions).

## Open Questions

- Should `fat_mass_kg`/`lean_mass_kg` be persisted columns, or recomputed on
  read from `weight_kg`/`body_fat_percentage` via the FOR-15 domain
  calculation? Recommend recomputing on read to avoid stored-vs-derived
  drift; confirm during implementation since the Jira summary does not
  specify.
- Exact migration filename/version depends on the latest migration present
  in the repo at implementation time (currently only `V1__baseline.sql`) —
  do not assume `V2` is still free without checking.
