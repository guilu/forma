# FOR-125 Test Plan

Strict TDD: failing tests first at each layer (domain → application → persistence → API), then implement.

## Scope

`Goal` + `Milestone` domain, persistence, and GET/POST/PATCH endpoints, with derived progress. Adherence/streak/achievements/photos/muscle-map are out of scope (later FOR-104 slices).

## Domain Tests

- `Goal` construction validates title/metric/target; rejects blank title and non-numeric target.
- Milestones preserved in order; completion state toggles.
- Progress derivation: given a linked metric + source value, computes `current`/`ratio` correctly; unlinked metric or no data → progress undefined/null, no exception.
- Metric with no source mapping is allowed and yields undefined progress.

## Application Tests

- Create → the goal is retrievable with its milestones.
- Update goal fields and milestone state via one PATCH path.
- Progress derivation reuses existing `BodyMeasurement`/`WeeklyCheckIn` data; no duplicated math.
- Owner-scoping: goals are only returned for the owner.

## Persistence Tests

- Round-trip a goal with milestones through the JDBC adapter against H2-in-PostgreSQL-mode with Flyway (V11).
- Legacy/empty DB → empty list, no error.

## API Tests

- `GET /goals` before any create → 200 `{"goals": []}`, never 404.
- `POST /goals` valid → 201/200; subsequent GET returns it with milestones and derived progress.
- `POST /goals` invalid metric / non-numeric target → 400 `VALIDATION_ERROR`.
- `PATCH /goals/{id}` unknown id → 404; valid → fields/milestone state updated.
- Response shape matches `api.md`, including explicit `null` progress when unlinked (present, not omitted, unless the codebase's NON_NULL convention dictates omission — match existing convention and assert accordingly).

## Edge Cases

- Goal metric with no data yet → progress null, goal still listed.
- Empty milestone list → allowed.
- Milestone target beyond goal target → allowed.

## Fixtures

- A goal whose metric maps to existing `BodyMeasurement` data (progress derivable) and one whose metric has no data (progress null).
- H2-in-PostgreSQL-mode with Flyway migrations for persistence/API integration tests, matching existing repository/controller test style (FOR-107/110).
