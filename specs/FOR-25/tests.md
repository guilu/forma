# FOR-25 Test Plan

## Scope

Verify the three strength workout templates: catalog references, per-item
sets/reps/rest/effort, and template structure.

## Domain Tests

- A `StrengthWorkout` template with ordered `StrengthWorkoutItem`s is created
  correctly.
- Each item carries `sets`, `repsMin`/`repsMax`, `restSeconds` and `rir`.
- An item referencing a non-existent catalog exercise is rejected.

## Application Tests

- Push, Pull and Legs & core templates all exist.
- Every template item references a valid FOR-24 catalog exercise id.
- If persisted, seeding is deterministic and idempotent.

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- A template with zero items (invalid).
- Duplicate `order` values within a template.
- A referenced exercise removed from the catalog (referential integrity).

## Fixtures

- The FOR-24 catalog (dependency) plus the three seeded templates.
- A minimal valid template + item for model-level unit tests.
