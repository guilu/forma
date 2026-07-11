# FOR-101: Classify BMI category server-side

Jira: https://dbhlab.atlassian.net/browse/FOR-101
Epic: FOR-95 UI Backend Enablers

## Summary

Classify a measurement's BMI into a category (e.g. Bajo peso / Saludable /
Sobrepeso / Obesidad) in the domain, and expose it in the body read model, so
the Mediciones UI (FOR-52) can show the "Saludable" badge without hardcoding
thresholds or making medical claims. Server-side, deterministic, non-diagnostic.

## User/System Flow

1. `GET /api/v1/body/measurements` returns each measurement with a `bmiCategory`
   derived from its `bmi`.
2. FOR-52 / FOR-51 render the category as a neutral badge.

## Functional Requirements

- Add a pure domain classifier mapping a BMI value → category with documented,
  standard thresholds (bounds inclusive/exclusive documented). Framework-free
  (ADR-001).
- Expose `bmiCategory` in `BodyMeasurementResponse` (FOR-17), derived from the
  measurement's `bmi`; `null` when `bmi` is absent.
- Copy is neutral and explicitly **non-medical** (docs/ui-guidelines.md) — a
  descriptive label, not advice or diagnosis.

## Non-Functional Requirements

- Deterministic: same BMI always yields the same category.
- No new persistence (derived on read from the existing `bmi`).

## Data Model Notes

`bmi` already exists on `BodyMeasurement` (nullable Double). Add a domain
classifier (e.g. `BmiCategory` enum + a `classify(Double bmi)` helper) and surface
the category string in the delivery `BodyMeasurementResponse`. No persisted field.

## Edge Cases

- `bmi` null → `bmiCategory` null (not a fabricated category).
- Boundary BMI values → land in exactly one category per the documented bounds.

## Open Questions

- Category set + exact thresholds: recommend the standard WHO adult bands
  (Bajo peso < 18.5; Saludable 18.5–24.9; Sobrepeso 25–29.9; Obesidad ≥ 30) with
  bounds documented — confirm labels/Spanish copy in implementation.
- Where to expose: recommend on `BodyMeasurementResponse` (per-measurement);
  document if a summary-level exposure is also wanted.
