# FOR-29 AI Context

## Story

FOR-29 — Create nutrition day template model
(https://dbhlab.atlassian.net/browse/FOR-29)

## Intent

Establish the Nutrition context's day-type target model before meals (FOR-31),
macro calc (FOR-32) and seeding (FOR-33) build on it. Success is a framework-free
`NutritionDayTemplate` with a constrained `type` and storable macro targets,
matching docs/domain-model.md.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Nutrition → NutritionDayTemplate)
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-22/`, `specs/FOR-24/` (domain model/enum precedents to follow)
- Jira: https://dbhlab.atlassian.net/browse/FOR-29

## Domain Notes

- `type` is a closed classification (`RUNNING | STRENGTH | REST`) — model as a
  Java `enum`, extensible later (as with `MeasurementSource`, `SessionType`).
- This type holds **targets**; the day's meals live in FOR-31 `MealTemplate`s
  that reference it. Keep them separate.

## Architectural Constraints

- Place the type under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`.
- No Spring, JPA/JDBC annotations or HTTP types (ADR-001). Backend uses JDBC +
  Flyway (no ORM) — no JPA entity to reuse.
- Any validation lives in the domain, not a controller.

## Common Pitfalls

- Free-form `type` string instead of an enum.
- Mixing food/brand fields into the day template (foods are FOR-30).
- Embedding meals here instead of referencing from FOR-31.

## Suggested Implementation Order

1. Define the `NutritionDayType` enum and the `NutritionDayTemplate` type.
2. Add construction-time validation for positive macro targets (per decision).
3. Unit-test creation, the constrained `type`, and validation.
4. Cross-check field names against docs/domain-model.md.

## Validation

Run backend unit tests (`./gradlew test` from `backend/`).
