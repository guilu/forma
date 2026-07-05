# FOR-16 AI Context

## Story

FOR-16 — Persist body measurements
(https://dbhlab.atlassian.net/browse/FOR-16)

## Intent

Give `BodyMeasurement` (FOR-15) a durable store so FOR-17 (API) and later
dashboard/graph stories can read real data. Success is a migration + JDBC
repository that saves and lists measurements correctly, verified by an
integration test.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md`
- `docs/adr/ADR-003-persistence.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/` (domain model this story persists)
- Jira: https://dbhlab.atlassian.net/browse/FOR-16

## Domain Notes

`BodyMeasurement` (FOR-15) is the type being persisted. Do not change its
domain contract from this story — only add a persistence adapter around it.

## Architectural Constraints

- Persistence adapters live under
  `backend/src/main/java/dev/diegobarrioh/forma/adapter/`; the repository
  port belongs to the application/domain boundary (ADR-001).
- Use JDBC (`spring-boot-starter-jdbc` + Flyway), not JPA — there is no ORM
  dependency in `backend/build.gradle`.
- New migrations go in
  `backend/src/main/resources/db/migration/`, numbered after the existing
  `V1__baseline.sql`.
- Do not let persistence concerns (row mapping, SQL types) leak into the
  FOR-15 domain type (ADR-003).

## Common Pitfalls

- Introducing a JPA/Hibernate dependency — the project deliberately stays on
  plain JDBC (see `backend/build.gradle` comment: "No JPA/ORM yet — the
  domain stays framework-free").
- Modifying or renumbering `V1__baseline.sql` instead of adding a new
  migration (ADR-003: avoid destructive migrations).
- Adding Withings/provider columns to `body_measurements`.
- Using floating-point column types for money/measurement precision instead
  of `NUMERIC`/`DECIMAL`.

## Suggested Implementation Order

1. Add the new Flyway migration creating `body_measurements`.
2. Define the repository port (save/list) at the application/domain
   boundary.
3. Implement the JDBC adapter mapping domain <-> row.
4. Add an integration test (save then list, ordering, and migration
   application) following `MigrationBaselineTest`'s pattern.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md Verification guidance,
"Persistence" row — FOR-83's migration baseline and test database setup
already exist in the repo, so this is not a "future command").
