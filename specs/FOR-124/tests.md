# FOR-124 Test Plan

## Scope

Verify week-over-week deltas render on related signals and the insights
history view lists and displays past periods correctly.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-110 contract; no backend change in this story.

## UI Tests

- Related signals show a delta value alongside the absolute value when the
  backend provides one.
- Related signals show only the absolute value (no delta) when the backend
  returns `null`/absent delta (first-ever week) — no "undefined" or
  fabricated zero.
- History view lists past periods, most recent first, from
  `GET /api/v1/insights/history`.
- Selecting a history entry renders that period's full insights (main +
  secondary + signals), reusing the same rendering as the current-week
  view.
- History fetch failure renders `ErrorState` with retry, independent of
  the current-week section's own state.
- Empty history (no past periods) renders an `EmptyState`, not an error.

## Edge Cases

- Delta sign/formatting is unambiguous (e.g. explicit `+`/`−`, not just
  color) for both positive and negative changes.
- A very long history list — pagination/scroll behavior decided during
  implementation and covered by a test once chosen.

## Fixtures

- Mocked `GET /api/v1/insights/history` with multiple periods, an empty
  history, and a current-week response with and without delta fields.
