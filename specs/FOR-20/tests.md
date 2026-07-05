# FOR-20 Test Plan

## Scope

Verify the progress graphs: correct series data from the API, the recent-
measurements default view, and empty/sparse-data states.

## Domain Tests

N/A — no domain logic in the frontend (ADR-006).

## Application Tests

N/A — no additional frontend state-management layer expected beyond the
page component and API client.

## API Tests

N/A — backend API tests are covered by `specs/FOR-17/tests.md`.

## UI Tests

- Weight graph renders a series built from `GET /api/v1/body/measurements`
  data.
- Body fat % graph renders a series from the same data.
- Lean mass is either rendered as a graph or its data series is prepared,
  per the chosen approach documented in spec.md/ai-context.md.
- Default view is limited to recent measurements (documented window), not
  the full history.
- Zero measurements shows the empty state, not an empty/broken chart.

## Edge Cases

- Exactly one measurement (documented single-point fallback behavior).
- Large date gaps between consecutive measurements.
- API error/network failure — page shows an error state, not a crash
  (ADR-006).

## Fixtures

- A mocked `GET /api/v1/body/measurements` response with several
  measurements spanning multiple weeks, to assert the recent-window
  behavior.
- A mocked empty-array response.
- A mocked single-item response.
