# FOR-20: Create body progress graphs

Jira: https://dbhlab.atlassian.net/browse/FOR-20
Epic: FOR-2 Body Composition

## Summary

Replace the current placeholder `ProgressPage`
(`frontend/src/pages/ProgressPage.tsx`) with simple graphs for weight, body
fat % and lean mass, using measurements from the FOR-17 API. Default view
focuses on recent measurements; keep it readable on mobile; no complex
filters in this MVP slice.

## User/System Flow

1. User opens the Progress page (already routed, currently a
   `PagePlaceholder`).
2. Frontend calls `GET /api/v1/body/measurements` and derives a
   recent-measurements series (e.g. most recent N points or a recent time
   window — see Open Questions) for weight, body fat % and lean mass.
3. Each metric renders as its own simple graph; if there is no data, an empty
   state is shown instead of an empty chart.

## Functional Requirements

- Weight graph and body fat % graph are visible and populated from API data.
- Lean mass is shown, or explicitly prepared (data series ready) if a full
  chart is deferred — per the story's AC wording ("lean mass shown or
  prepared").
- Default view shows recent measurements only; no date-range pickers,
  comparisons or export/filter controls in this MVP slice.
- Readable on mobile (docs/ui-guidelines.md: "avoid noisy charts").
- Chart uses live API data when available, per the story's DoD — no
  hardcoded/sample series in the shipped component.

## Non-Functional Requirements

- No domain calculation duplicated client-side; lean mass/body fat values
  come from the API response as-is.
- Empty state required when there are zero (or too few, e.g. one)
  measurements to plot meaningfully.

## Data Model Notes

Graph series are built directly from `GET /api/v1/body/measurements`
response items (`specs/FOR-17/api.md`): `measuredAt` as the x-axis, and
`weightKg` / `bodyFatPercentage` / `leanMassKg` as the three series values.

## Edge Cases

- Zero measurements — empty state, no chart axes with no data.
- Exactly one measurement — a single point is not a meaningful line graph;
  decide and document the chosen fallback (e.g. show the single value as a
  stat instead of a line, or show a single-point chart).
- Large gaps between measurement dates — chart should not imply a false
  trend between distant points; keep the default view honest about sparse
  data.

## Open Questions

- **No frontend chart library is selected yet.** `frontend/package.json`
  currently has no charting dependency (only `react`, `react-dom`,
  `react-router-dom` as runtime deps). The Jira summary says "use the
  project's selected frontend chart library," but none exists in the repo
  at spec time. Per AGENTS.md ("if a story references future functionality
  that is not present yet, document it as planned instead of creating it
  early" / "repository state has priority"), this is a gap to resolve during
  implementation: either pick and add a lightweight charting dependency (and
  record that choice, e.g. as an ADR or a note here) or implement the MVP
  graphs with plain SVG/CSS without a new dependency. Do not silently assume
  a library.
- Exact "recent measurements" window (last N points vs. last N weeks) is not
  specified by Jira; pick a simple, documented default (e.g. last 12 points)
  consistent with "avoid complex filters in MVP."
