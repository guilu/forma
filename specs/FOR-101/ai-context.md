# FOR-101 AI Context

## Story

FOR-101 — Classify BMI category server-side
(https://dbhlab.atlassian.net/browse/FOR-101)

## Intent

Let the UI show an IMC category badge ("Saludable") without client thresholds or
medical claims. Success is a pure domain classifier + a `bmiCategory` field in the
body read model, derived from the existing `bmi`.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (calm, non-alarming, non-medical),
  `docs/api/body-measurements.md`
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/` (BodyMeasurement, has `bmi`), `specs/FOR-17/` (API)
- Jira: https://dbhlab.atlassian.net/browse/FOR-101

## Domain Notes

- `bmi` already exists on `BodyMeasurement` (nullable). Do NOT recompute BMI —
  classify the stored value.
- Classifier is pure/deterministic; null-safe (null bmi → null category).
- Expose the category in `delivery/body/BodyMeasurementResponse` (`@JsonInclude`
  drops it when null).

## Architectural Constraints

- Framework-free domain classifier (ADR-001). No persistence. DTO carries the
  category string (ADR-005). Non-diagnostic copy.

## Common Pitfalls

- Hardcoding thresholds in the frontend (this story moves them server-side).
- Medical/diagnostic wording.
- Fabricating a category when `bmi` is null.

## Suggested Implementation Order

1. `BmiCategory` domain type + `classify(Double bmi)` with documented thresholds
   (+ domain tests incl. boundaries and null).
2. Add `bmiCategory` to `BodyMeasurementResponse.from(...)`.
3. API test asserting the category for known BMIs and null when absent.

## Validation

Run `./gradlew test spotlessApply` from `backend/`. Verify categories for known
BMIs and that a null `bmi` yields a null category.
