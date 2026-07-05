# FOR-16 Test Plan

## Scope

Verify the Flyway migration, repository port and JDBC adapter for
`BodyMeasurement` persistence: save, list, ordering, and precision.

## Domain Tests

N/A — the domain type is covered by FOR-15; this story adds persistence
around it without changing domain behavior.

## Application Tests

- Repository `save()` persists a measurement that can be read back with
  equivalent values.
- Repository `list()` returns measurements ordered by `measured_at`
  descending by default.
- Migration applies cleanly to a fresh database (extend the pattern in
  `MigrationBaselineTest`).
- Numeric precision is preserved across save/read for `weight_kg`,
  `body_fat_percentage` and `bmi`.

## API Tests

N/A — the API arrives in FOR-17.

## UI Tests

N/A — no UI in this story.

## Edge Cases

- Two measurements with the same `measured_at` — list ordering is stable and
  documented.
- `notes` absent (nullable column round-trips correctly).
- Migration re-run is a no-op (Flyway history already applied).

## Fixtures

- At least two `body_measurements` rows with distinct `measured_at` values to
  assert descending order.
- One row with `notes` null to assert optional-field round-trip.
