# ADR-010: Charting approach

## Status

Accepted

## Context

FOR-20 introduces the first data visualizations in FORMA: simple body-progress
line graphs (weight, body fat %, lean mass) on the Progress page. The Jira story
refers to "the project's selected frontend chart library", but no charting
dependency exists in `frontend/package.json` — the runtime deps are only
`react`, `react-dom` and `react-router-dom`. A decision was required before
writing the graphs (spec FOR-20 Open Questions).

The MVP need is deliberately small: a few read-only line charts of recent
measurements, no zoom, filters, date-range pickers or comparisons.
`docs/ui-guidelines.md` explicitly asks to "avoid noisy charts".

## Decision

Render the MVP body-progress graphs with **plain inline SVG**, adding **no new
charting dependency**.

A small in-house `LineChart` component maps a measurement series to an SVG
polyline with point markers and minimal axis labels.

## Consequences

- No bundle-size or supply-chain cost from a charting library for a handful of
  simple line charts.
- Full control over accessibility (text alternative, ARIA) and the FORMA visual
  language (design tokens, restrained accent) without fighting a library's
  defaults.
- Advanced charting (interactive tooltips, brushing, many series, axis scaling
  edge cases) is not free — but it is explicitly out of scope for this MVP slice.
- If a later story needs richer, interactive charts, revisit this decision and
  introduce a vetted library then (superseding this ADR), rather than pre-adding
  one now.

## Rules

- Charts render real API data only — no hardcoded or sample series in shipped
  components (FOR-20 DoD).
- Every chart has a text alternative so the trend is not communicated by color
  alone (ADR-006 accessibility).
- Keep charts simple and readable on mobile (`docs/ui-guidelines.md`: avoid
  noisy charts); prefer honest rendering of sparse data over dense visuals.
- Do not duplicate backend-derived values (e.g. lean mass) client-side; plot
  what the API returns.
