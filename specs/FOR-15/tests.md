# FOR-15 Test Plan

## Scope

Unit-test the `BodyMeasurement` domain type and its derived-value
calculations. No persistence, API or UI is introduced by this story.

## Domain Tests

- `fatMassKg`/`leanMassKg` computed correctly from `weightKg` +
  `bodyFatPercentage`.
- Derived values stay consistent when re-derived from the same inputs
  (no rounding drift between the two formulas).
- `notes` is optional and never affects calculation.
- `source` defaults/required behavior matches the decision recorded in
  spec.md Open Questions.

## Application Tests

N/A — no application/use-case layer is introduced by this story.

## API Tests

N/A — the API arrives in FOR-17.

## UI Tests

N/A — the UI arrives in FOR-18/FOR-19/FOR-20.

## Edge Cases

- `bodyFatPercentage` missing/null.
- `bodyFatPercentage` at boundary values (`0`, `100`).
- `weightKg` zero or negative.

## Fixtures

- A "normal" measurement (all fields present).
- A measurement missing `bodyFatPercentage` (optional-field case).
- A boundary measurement (`bodyFatPercentage` at `0` or `100`).
