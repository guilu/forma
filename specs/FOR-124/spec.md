# FOR-124: Progress: insights history view + WoW deltas

Jira: https://dbhlab.atlassian.net/browse/FOR-124
Epic: FOR-47 UI & UX

## Summary

`InsightsSection.tsx`'s doc comment documents two gaps explicitly: "no
persisted insight history exists — FOR-45 is computed on demand with no
history endpoint... so a 'historical insights list' is deferred entirely
rather than built against nothing. This section always shows the current
week only," and "'Related signals' render the check-in's latest absolute
values... there is no week-over-week 'delta' field on `WeeklyCheckIn`... so
this shows the signals the recommendation was computed from, not a trend."
FOR-110 (backend) adds both a history endpoint and delta fields. This story
builds the frontend: a past-weeks insights view and week-over-week delta
display on related signals.

## User/System Flow

1. User opens Progreso (`/progreso`); the existing `InsightsSection`
   continues to show the current week's `main`/`secondary`
   recommendations and disclaimer, unchanged.
2. "Related signals" now show week-over-week deltas (e.g. "Peso: 78.2 kg
   (−0.4 kg vs. semana anterior)") alongside the existing absolute values,
   when a prior period exists.
3. A new history view (within Progreso, or a distinct entry point —
   consistent with FOR-56's existing decision to keep insights inside
   Progreso rather than adding a new nav item) lists past weeks' insights,
   most recent first.

## Functional Requirements

- **WoW deltas**: read the new delta fields from `WeeklyCheckIn` (FOR-110)
  and render them alongside each related signal's current absolute value;
  `null` delta (no prior period) → show the absolute value only, exactly
  as today, not a fabricated "no change" indicator.
- **History view**: consume `GET /api/v1/insights/history` (FOR-110); list
  past periods (period label + `main` recommendation summary at minimum);
  selecting an entry shows that period's full insights (mirroring the
  current week's existing rendering, reused for a historical period).
- Reuse `StatusPill` for severity in the history view exactly as the
  current-week view already does, for visual/copy consistency
  (`docs/api/weekly-insights.md`, ui-guidelines.md).
- No client-side delta computation and no client-side history
  reconstruction — every value rendered comes directly from FOR-110's
  response (architecture-overview.md).

## Non-Functional Requirements

- No regression to the existing current-week insights rendering, its
  disclaimer, or its "no new nav item" decision (FOR-56 Open Question,
  already resolved) — history lives inside the existing Progreso
  structure, not a new top-level nav entry, unless FOR-110/this story's
  implementation finds a strong reason to reconsider (document if so).
- Loading/empty/error states (FOR-60) for the history fetch, independent
  from the current-week fetch's own states.

## Data Model Notes

Consumes FOR-110's history endpoint and the delta-enriched
`WeeklyCheckIn`/`WeeklyInsights` response. `frontend/src/api/insights.ts`
types need new fields (delta values, possibly a distinct
`HistoricalInsights` type or a reused `WeeklyInsights` with a `period`
field) mirroring FOR-110's shape.

## Edge Cases

- No history yet (first week ever, or history endpoint returns empty) →
  history view shows an empty state, not an error.
- First-ever week (no prior period) → deltas absent on signals, matching
  today's absolute-only rendering; verify this doesn't regress into
  showing a broken "undefined" delta.
- A gap week in history (per FOR-110's documented comparison rule) →
  render the delta FOR-110 actually computed, without the UI re-deriving
  its own comparison across the gap.

## Open Questions

- Whether the history view is a separate route/section within Progreso or
  an expandable list within the existing `InsightsSection` — recommend
  keeping it within Progreso per FOR-56's precedent, exact placement
  decided during implementation once FOR-110's history payload size/shape
  is known.
