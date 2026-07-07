# FOR-24 Test Plan

## Scope

Verify the `Exercise` model and the initial catalog seed: constrained
enums, home-equipment-only rule, and push/pull/legs/core coverage.

## Domain Tests

- An `Exercise` is created with valid `movementPattern` and `equipment`.
- `movementPattern` and `equipment` accept only the known values (enums).
- Each catalog exercise declares required equipment from the supported home set.

## Application Tests

- The initial seed includes at least one exercise for each of push, pull,
  legs (squat/hinge) and core.
- No seeded exercise uses non-home (machine/gym) equipment.
- If persisted, seeding is deterministic and idempotent.

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Duplicate exercise names in the seed (per the decision in spec.md).
- An exercise with empty `primaryMuscles`/`instructions`.
- Equipment value outside the supported set (must be impossible via enum).

## Fixtures

- The seeded catalog itself.
- A minimal valid `Exercise` for model-level unit tests.
