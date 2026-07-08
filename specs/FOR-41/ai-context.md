# FOR-41 AI Context

## Story

FOR-41 — Create recommendation model
(https://dbhlab.atlassian.net/browse/FOR-41)

## Intent

Define the explainable output of the Insights engine: every suggestion carries a
category, severity, a short message and a data-referencing reason. Success is a
constrained, framework-free `Recommendation` used by all FOR-42/43/44 rules and
the FOR-45 API.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Insights → Recommendation)
- `docs/ui-guidelines.md` (neutral, non-gamified copy)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- Jira: https://dbhlab.atlassian.net/browse/FOR-41

## Domain Notes

- `category` and `severity` are closed sets → Java `enum`s.
- Category set follows the Jira story (`BODY | TRAINING | NUTRITION | RECOVERY |
  SHOPPING`), which differs from docs/domain-model.md's `RUNNING`/`STRENGTH` —
  document the mapping (BODY↔body, TRAINING↔running+strength).
- Explainability is core: `reason` must reference observed data; no fake
  precision, no medical claims.

## Architectural Constraints

- Type in `.../domain/`, framework-free (ADR-001). No persistence.
- Construction-time validation for required `message`/`reason`.

## Common Pitfalls

- Free-form `category`/`severity` strings instead of enums.
- A recommendation without a `reason` (must include both message and reason).
- Alarming or prescriptive-medical copy.

## Suggested Implementation Order

1. Define `RecommendationCategory` and `RecommendationSeverity` enums.
2. Define the `Recommendation` record with validation (message + reason
   required; optional related metric).
3. Unit-test creation, the constrained enums, and required fields.

## Validation

Run `./gradlew test` from `backend/`.
