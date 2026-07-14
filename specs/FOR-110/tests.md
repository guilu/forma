# FOR-110 Test Plan

## Scope

Verify insights are persisted on generation, a history endpoint returns
past periods, and week-over-week deltas are computed correctly against the
prior period.

## Domain Tests

- Delta computation between two `WeeklyCheckIn` snapshots yields the
  expected weight/body-fat/lean-mass/training-count deltas.
- Delta computation with no prior snapshot yields `null` deltas, not zeros.

## Application Tests

- `WeeklyInsightsService` (or its persistence-aware extension) persists the
  generated `WeeklyInsights` keyed by period.
- History use case returns persisted periods ordered most recent first.
- History use case returns an empty list before any insights have been
  generated.
- Current-week generation still returns the same core fields (`checkIn`,
  `main`, `secondary`, `generatedAt`) as before this story (regression
  check against FOR-45/FOR-56).

## API Tests

- `GET /api/v1/insights/history` before any generation → 200, empty list.
- Generate insights for two consecutive weeks, then `GET history` → both
  periods present, most recent first.
- Current-week `GET` response includes delta fields; first-ever week has
  `null` deltas.
- Existing FOR-45/FOR-56 current-week fields unchanged (regression check).

## UI Tests

N/A — backend story.

## Edge Cases

- A gap week between two generated periods → delta compares against the
  most recent prior persisted period, not an intermediate missing one.
- Repeated generation within the same period (re-running the same week) →
  documented behavior (overwrite vs. append); test whichever is chosen.

## Fixtures

- Two or more consecutive weeks of persisted `WeeklyCheckIn` data for delta
  and history tests.
- A single-week fixture (no prior period) for the null-delta test.
