# FOR-21: Create weekly body check-in summary (backend)

Jira: https://dbhlab.atlassian.net/browse/FOR-21
Epic: FOR-2 Body Composition

## Summary

Compute a weekly body summary from available `BodyMeasurement` data (FOR-15/
FOR-16): latest weight, latest body fat %, latest lean mass, weekly weight
change, and weekly body fat change when enough data exists. First version is
rule-based and simple; it must not claim precision it doesn't have and must
degrade to a clear message when data is insufficient.

## User/System Flow

1. An application use case reads recent measurements via the FOR-16
   repository.
2. It computes: latest weight/body fat %/lean mass (most recent measurement),
   plus weekly weight change and weekly body fat change when a comparable
   prior measurement exists.
3. It produces a summary result including a short human-readable status
   message, exposed for later dashboard/API consumption (docs/domain-model.md
   already sketches a related `WeeklyCheckIn` concept in the Insights
   context — this story is the Body-side summary feeding that, not the full
   Insights engine).

## Functional Requirements

- Latest weight, latest body fat %, latest lean mass come from the most
  recent `BodyMeasurement` (via FOR-16 repository ordering).
- Weekly weight change and weekly body fat change are computed only when a
  suitable prior-week measurement exists; otherwise those fields are absent/
  null rather than a fabricated `0` (AC: "handles missing previous-week
  data").
- Keep the calculation rule-based and simple for this first version — no
  statistical modeling, no forecasting.
- Result includes a short, human-readable status message (docs/ui-guidelines.md
  tone: e.g. "Body fat is down 0.3% over the last 2 weeks while weight is
  stable" — factual, not "gamified"/exaggerated language, per
  docs/ui-guidelines.md "Interaction style").
- The summary is computed in the domain/application layer, not in a
  controller (ADR-001) — no new HTTP endpoint is required by this story
  unless the story is later paired with one; this story's scope is the
  backend computation and its exposure as data for FOR-19/dashboard use.

## Non-Functional Requirements

- Do not claim a trend/precision the data doesn't support (e.g. do not
  report a "weekly change" from measurements taken days apart in an
  irregular pattern without saying so).
- Deterministic: same input measurements always produce the same summary.

## Data Model Notes

Builds on `BodyMeasurement` (FOR-15/FOR-16) only; does not introduce a new
persisted entity in this story. If a summary needs to be queried repeatedly
rather than recomputed, that persistence decision is out of scope here (see
Open Questions) — docs/domain-model.md's `WeeklyCheckIn` is a related but
separate Insights-context concept and is not created by this story.

## Edge Cases

- Zero measurements — summary reports "not enough data" rather than nulls
  with no explanation.
- Exactly one measurement — latest values are available; weekly changes are
  not (no prior week to compare).
- Two measurements more than a week apart — "weekly change" should not
  silently compare across a longer gap as if it were exactly one week;
  reflect the actual comparison window in the status message or omit the
  change if it would be misleading.

## Open Questions

- Should the weekly summary be a computed-on-demand value (recomputed from
  `BodyMeasurement` each time it's requested) or a persisted snapshot? Jira
  does not require persistence and the story's AC only says "exposes data
  for dashboard use" — recommend computed-on-demand for this first version
  to avoid a new migration/table not requested by the story; revisit if a
  later story needs historical weekly summaries.
- Exact "prior week" comparison rule (e.g. nearest measurement 5-9 days
  prior, vs. a fixed week-boundary lookup) is not specified — pick a simple,
  documented rule and note it in code comments/tests.
