# FOR-15 AI Context

## Story

FOR-15 — Create body measurement domain model
(https://dbhlab.atlassian.net/browse/FOR-15)

## Intent

Establish the Body bounded context's core domain type before persistence
(FOR-16), API (FOR-17) and UI (FOR-18/19/20) work starts on top of it. Success
is a framework-free `BodyMeasurement` whose derived-value calculation is
correct, unit-tested, and matches docs/domain-model.md.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md`
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- Jira: https://dbhlab.atlassian.net/browse/FOR-15

## Domain Notes

- `BodyMeasurement` is the Body context's core entity (docs/domain-model.md).
- `source` is a classification marker, not a payload carrier — provider
  details belong to Integrations adapters, never to this model
  (docs/architecture-overview.md, "Integration model").

## Architectural Constraints

- Place the type under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`.
- No Spring, JPA/JDBC annotations or HTTP types in this package (ADR-001).
- Derived-value calculation logic belongs in the domain, not in a controller
  or repository.

## Common Pitfalls

- Adding `muscleMassKg`/`waterPercentage` or other Withings-only fields — out
  of scope for this story (see spec.md Data Model Notes).
- Coupling `BodyMeasurement` to a persistence row: the backend uses JDBC, not
  JPA (`backend/build.gradle` — `spring-boot-starter-jdbc` + Flyway, no ORM),
  so there is no JPA entity to reuse or extend.
- Returning `0` for a missing `bodyFatPercentage` instead of a defined, tested
  behavior for the derived fields.

## Suggested Implementation Order

1. Define the domain type and the `source` classification.
2. Implement derived-value calculation (`fatMassKg`, `leanMassKg`).
3. Add unit tests for the calculation and the edge cases in spec.md.
4. Cross-check the field list against docs/domain-model.md.

## Validation

Run backend unit tests for the new domain package
(`./gradlew test` from `backend/`, per AGENTS.md's Verification guidance
"Backend" row — the Gradle backend already exists at `backend/`).
