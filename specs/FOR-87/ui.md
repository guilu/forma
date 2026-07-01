# UI Notes: FOR-87

## Applicability

This story validates frontend test infrastructure rather than product UI.

## Requirements

- Provide examples that future UI stories can copy.
- Support rendering/state validation for components.
- Support basic interaction testing where practical.
- Keep tests isolated from real backend services.

## Constraints

- Do not create product screens for the sake of testing.
- Avoid snapshot-only examples as the main testing pattern.
- Do not encode domain calculations in frontend tests.

## Future-Proofing

Later UI stories should add tests for loading, empty, error and interaction states following this baseline.
