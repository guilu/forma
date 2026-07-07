# FOR-29 Test Plan

## Scope

Unit-test the `NutritionDayTemplate` domain type: creation, the constrained
`type`, and macro-target validation. No persistence, API or UI in this story.

## Domain Tests

- A valid day template is created with all fields set correctly.
- `type` only accepts `RUNNING`, `STRENGTH`, `REST` (enforced by the enum).
- Macro targets are stored and read back correctly.
- Construction-time validation rejects invalid macro targets (e.g. negative
  calories/protein), if that decision is taken.

## Application Tests

N/A — no application/use-case layer is introduced by this story.

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Negative or zero macro targets.
- `notes` absent (optional field).

## Fixtures

- A "normal" running-day template with realistic macro targets.
- A rest-day template with lower carbohydrate target (to assert distinct
  values coexist).
