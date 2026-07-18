# FOR-155 Test Plan

Strict TDD: failing tests first (domain record → migration/persistence → API/empty state), then implement.

## Scope

The weekly tracking record domain type, its persistence + migration, and the create/list/read API,
including the empty-start behavior.

## Domain Tests

- `WeeklyTrackingRecord` accepts a full *Seguimiento* row (week 1 values); validates positive weight and % bounds; rejects invalid values.
- Partial record (some fields null) is valid.
- Kcal-recomendadas behavior per the chosen design (derived from FOR-149 base kcal, or accepted as input).

## Application / Persistence Tests

- Weekly record round-trips through the persistence adapter (all *Seguimiento* fields).
- Migration creates the weekly table additively above V19; no seed rows inserted (starts empty).
- One record per week (dedupe/upsert) behaves as documented.

## API Tests

- `POST /api/v1/tracking/weekly` creates a record; `GET` returns it.
- `GET /api/v1/tracking/weekly` on a fresh install returns `[]` (200, empty state), not 404.
- `GET /api/v1/tracking/weekly/{week}` for a missing week → 404.
- Invalid payload → 400.

## Edge Cases

- Empty collection (default) → empty state, no error.
- Partial week (body metrics without km, or km without body metrics).
- Fields available for FOR-150 rules 4–5 (pace/comment; HR if added).

## Fixtures

- Week-1 *Seguimiento* row as the create fixture (peso 73.6, grasa 14.7, km 13.0, etc.).
- Empty database (no seed) for the empty-state test.
- H2-in-PostgreSQL-mode with Flyway running all migrations including the new `V<N>`.
