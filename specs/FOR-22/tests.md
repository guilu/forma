# FOR-22 Test Plan

## Scope

Unit-test the `RunningPlanSession` domain type: creation, the constrained
`sessionType`, and multi-week support. No persistence, API or UI in this story.

## Domain Tests

- A valid planned session is created with all fields set correctly.
- `sessionType` only accepts the known values (`EASY`, `LONG_RUN`,
  `INTERVALS`, `RECOVERY`) — enforced by the `enum` type.
- Sessions across different `weekNumber` values coexist (multi-week plan).
- Construction-time validation rejects invalid values per spec.md (e.g.
  non-positive `targetDistanceKm`), if that decision is taken.

## Application Tests

N/A — no application/use-case layer is introduced by this story.

## API Tests

N/A — no HTTP endpoint in this story.

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- `targetDistanceKm` zero or negative.
- `weekNumber` at the boundary (e.g. 1, or an invalid 0).
- `notes` absent (optional field).

## Fixtures

- A "normal" easy-run planned session.
- A long-run session in a later week (to assert multi-week support).
