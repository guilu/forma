# FOR-22 AI Context

## Story

FOR-22 — Create running plan domain model
(https://dbhlab.atlassian.net/browse/FOR-22)

## Intent

Establish the Training context's core planned-run type before seeding (FOR-23),
calendar (FOR-26) and completion (FOR-27) build on it. Success is a
framework-free `RunningPlanSession` with a constrained `sessionType` and
multi-week support, unit-tested, matching docs/domain-model.md.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Training → RunningPlanSession)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/` (BodyMeasurement — the domain-model precedent to follow)
- Jira: https://dbhlab.atlassian.net/browse/FOR-22

## Domain Notes

- `RunningPlanSession` is a **planned** session (a plan entry), not a logged
  run (`RunningSession`). Do not conflate them.
- `sessionType` is a closed classification (`EASY | LONG_RUN | INTERVALS |
  RECOVERY`) — model as a Java `enum`, extensible later without breaking the
  contract (same approach as FOR-15 `MeasurementSource`).

## Architectural Constraints

- Place the type under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`.
- No Spring, JPA/JDBC annotations or HTTP types (ADR-001). Backend uses JDBC +
  Flyway, no ORM (`backend/build.gradle`), so there is no JPA entity to reuse.
- Any construction-time validation lives in the domain, not a controller.

## Common Pitfalls

- Modelling `sessionType` or `dayOfWeek` as a free-form `String` instead of an
  `enum` / `java.time.DayOfWeek`.
- Adding actual-run fields (heart rate, calories, actual pace) — those belong to
  the future `RunningSession`, not this planned type.
- Coupling the model to a persistence row (persistence is FOR-23).

## Suggested Implementation Order

1. Define the `SessionType` enum and the `RunningPlanSession` type.
2. Add construction-time validation for the documented rules.
3. Unit-test creation, the constrained `sessionType`, and multi-week support.
4. Cross-check field names against docs/domain-model.md.

## Validation

Run backend unit tests (`./gradlew test` from `backend/`, AGENTS.md "Backend"
row — the Gradle backend already exists).
