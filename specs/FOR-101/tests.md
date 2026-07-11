# FOR-101 Test Plan

## Scope

Verify BMI classification (thresholds, boundaries, null-safety) and its exposure
in the body read model.

## Domain Tests

- Known BMI values map to the expected category (one per band).
- Boundary values (e.g. 18.5, 25.0, 30.0) land in exactly one category per the
  documented bounds.
- Null BMI → null/absent category (not fabricated).

## Application Tests

N/A — classification is pure domain, exposed via the DTO.

## API Tests

- `GET /api/v1/body/measurements` includes `bmiCategory` for a measurement with a
  BMI.
- A measurement without a BMI omits `bmiCategory` (`@JsonInclude`).

## UI Tests

N/A — backend story.

## Edge Cases

- Boundary BMIs classified deterministically.
- Missing BMI → no category.

## Fixtures

- Measurements spanning each BMI band + one with null BMI.
