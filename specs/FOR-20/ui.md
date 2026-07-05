# FOR-20 UI Spec

## Screens

- Progress page (`frontend/src/pages/ProgressPage.tsx`) — currently a
  `PagePlaceholder`; this story adds the graphs.

## Components

- Weight graph.
- Body fat % graph.
- Lean mass graph (or prepared data series, if a full chart is deferred —
  see spec.md).
- Empty-state component for zero/insufficient data.

## States

- Loading: graphs area shows a loading indicator while fetching.
- Empty: zero measurements shows a clear message instead of empty chart
  axes.
- Error: API/network failure shows an error state (ADR-006).
- Success: graphs populated with the recent-measurements window.

## Interactions

- No filters/date-range pickers in this MVP slice (explicit non-goal in the
  Jira summary).
- Default view is read-only; any drill-down/detail interaction is out of
  scope unless trivially available from the chosen chart approach.

## Accessibility

- Graphs have a text alternative (e.g. a visually-hidden summary or adjacent
  labeled values) so the trend is not communicated by color alone.
- Empty/error states are announced to screen readers.

## Responsive Behavior

- Mobile: graphs must stay readable on small screens — docs/ui-guidelines.md
  explicitly says "avoid noisy charts"; prefer simpler rendering over dense
  desktop-only visuals.
- Desktop: may show slightly more detail (e.g. more points in the recent
  window) but must use the same underlying data/series logic as mobile.
