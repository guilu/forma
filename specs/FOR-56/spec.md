# FOR-56: Create insights and recommendations screens

Jira: https://dbhlab.atlassian.net/browse/FOR-56
Epic: FOR-47 UI & UX

## Summary

Build the insights/recommendations UI: the current weekly insight, an explanation
of *why* it was generated, the related body/training/nutrition signals, a history
list, a severity indicator and a clear non-medical disclaimer. Data source is the
FOR-45 weekly insights endpoint. Insights must be calm and explainable — "no fake
AI oracle energy". Related mockup: `docs/6-progreso.png` (progress screen showing
adherence + recommendation-style cards).

## User/System Flow

1. The main recommendation surfaces on the dashboard (FOR-51 widget) and in a
   fuller insights view.
2. The view loads `GET /api/v1/insights/weekly` (FOR-45): check-in summary + main
   + secondary recommendations + generated timestamp.
3. User reads the recommendation, its reason and the signals behind it.

## Functional Requirements

- **Current weekly insight card**: the FOR-45 main recommendation (message +
  severity + reason).
- **Explanation**: render the reason/why text from the read model (do not
  synthesize new rationale in the UI).
- **Related signals**: the check-in evidence (body deltas, training completion)
  that back the recommendation.
- **Secondary recommendations**: the FOR-45 secondary list.
- **Severity/priority indicator**: `INFO` / `WARNING` / `ACTION` badge (FOR-50).
- **Non-medical disclaimer**: clear, calm, non-diagnostic copy.
- **Historical insights list**: only if a history endpoint exists; otherwise show
  the current week and document the gap (FOR-45 is computed-on-demand, no
  persistence).
- No-insight/empty and error states (FOR-60).

## Non-Functional Requirements

- Calm, explainable, non-alarming copy (docs/ui-guidelines.md).
- Explainability text comes from the backend read model; UI does not invent it.

## Data Model Notes

Consumes FOR-45 (`WeeklyInsightsResponse`: `checkIn`, `main`, `secondary`,
`generatedAt`). **Not backed yet**: a persisted history of past insights (FOR-45
is on-demand only) — the "historical insights" requirement is deferred/documented
until a history endpoint exists. Progress-screen extras in `docs/6-progreso.png`
(progress photos, achievements, streaks) belong to progress/goals, not insights.

## Edge Cases

- Empty data → FOR-45 returns an insufficient-data `INFO` main; render it, no
  error.
- Missing secondary recommendations → show only the main.
- No history endpoint → hide/deferred history section, not a broken list.

## Open Questions

- The nav has no dedicated "Insights" section (it has Progreso/Objetivos); the
  ui-guidelines list suggests "Insights". Recommend surfacing insights via the
  dashboard widget + within Progreso for the MVP; document whether a dedicated
  route is added.
- Historical insights need backend persistence — document as a future story.
