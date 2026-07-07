# FOR-28: Create weekly training summary

Jira: https://dbhlab.atlassian.net/browse/FOR-28
Epic: FOR-3 Training Engine

## Summary

Compute a weekly training summary from the plan (FOR-22/FOR-23/FOR-25) and
completion status (FOR-27): planned vs. completed running sessions, planned vs.
completed strength sessions, total planned running distance, and completed
running distance when available. Rule-based and simple; handles the empty state.
Backend computation for later dashboard use.

## User/System Flow

1. An application use case reads the week's planned sessions and their
   completion status via the repository/read model.
2. It computes the summary counts and distances.
3. It produces a summary result the dashboard can display (FOR-28 result
   exposed as data; no product endpoint required by this story unless paired
   with one — see Open Questions).

## Functional Requirements

- Compute, for the current week: planned running sessions, completed running
  sessions, planned strength sessions, completed strength sessions.
- Compute total planned running distance, and completed running distance when
  available (only from sessions marked completed).
- Keep the calculation rule-based and simple (counts + sums); no statistics/
  forecasting.
- Compute in the domain/application layer (ADR-001), not a controller; follow
  the FOR-21 `WeeklyBodySummary` precedent (pure domain calc + application
  service reading via a repository/port).
- Handle the empty state: no planned sessions → a clear "nothing planned"
  result, not zero-filled fields with no explanation.

## Non-Functional Requirements

- Deterministic: same plan + completion state always yields the same summary.
- Do not overstate: completed running distance reflects only completed
  sessions; if completion data is unavailable, that field is absent/null, not a
  fabricated value.

## Data Model Notes

Builds on FOR-22/FOR-23/FOR-25 (plan) and FOR-27 (status). Computed on demand;
introduces no persisted summary entity (Body precedent: `WeeklyBodySummary` in
FOR-21). Distinct from the Insights-context concepts in docs/domain-model.md.

## Edge Cases

- No planned sessions this week — empty-state result.
- Sessions planned but none completed — completed counts are 0 (0 is correct
  here, since sessions exist), completed distance 0/absent per availability.
- Completed running session without a recorded distance — count it as completed
  but omit its distance from the completed-distance sum (document the rule).

## Open Questions

- **Exposure**: computed-on-demand value (FOR-21 precedent) with no new
  endpoint, vs. a dedicated dashboard endpoint. Recommend computed-on-demand
  service for this story; a display endpoint is separate/explicit work.
- **"Current week" definition**: calendar week vs. plan week — pick a simple,
  documented rule consistent with FOR-26.
