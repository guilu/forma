# FOR-110: [STUB] Insight history persistence + week-over-week deltas

Jira: https://dbhlab.atlassian.net/browse/FOR-110
Epic: FOR-96

## Summary

FOR-45's weekly insights are computed on demand with no persisted history
(`WeeklyInsightsService` builds a fresh `WeeklyInsights` every call;
documented gap in `specs/FOR-56/spec.md` Data Model Notes and confirmed in
`InsightsSection.tsx`'s doc comment: "No persisted insight history exists").
This story persists each generated insight with its period, adds an
endpoint to list past weeks' insights, and adds week-over-week delta fields
to the `WeeklyCheckIn` signals — also a documented gap ("there is no
week-over-week 'delta' field on `WeeklyCheckIn`"). Unblocks FOR-124
(insights history view + WoW deltas).

## User/System Flow

1. On each weekly insights generation (FOR-45's existing on-demand compute
   path), the backend persists the resulting `WeeklyInsights` keyed by its
   period (week start date).
2. Client calls a new endpoint (e.g. `GET /api/v1/insights/history`) to list
   past weeks' persisted insights.
3. `WeeklyCheckIn`'s signals gain delta fields (e.g. weight change vs. prior
   week, body fat % change, training completion count change) computed by
   comparing the current period's snapshot to the prior persisted one.

## Functional Requirements

- **Persist on generate**: when `WeeklyInsightsService` produces a
  `WeeklyInsights`, store it (period, `checkIn`, `main`, `secondary`,
  `generatedAt`) rather than only returning it transiently.
- **History endpoint**: expose past weeks' persisted insights, ordered most
  recent first, with the same shape as the current-week response (FOR-45)
  plus the period it belongs to.
- **Week-over-week deltas**: add delta fields to `WeeklyCheckIn` (or a
  wrapping read model) — weight change, body fat % change, lean mass
  change, training completion count change — computed against the
  immediately prior persisted period. `null`/absent when there is no prior
  period to compare against.
- Keep the existing current-week `GET` insights endpoint (FOR-45/FOR-56)
  working exactly as today; this story is additive.
- Domain-first: persistence is an adapter behind the existing insights
  application boundary (ADR-001); DTOs distinct from domain (ADR-005).

## Non-Functional Requirements

- Migration-driven schema for the new insight-history storage (ADR-003).
- Additive/backward compatible — the current-week endpoint's existing
  fields are unchanged.
- No client-side delta computation — the frontend renders what the backend
  returns (architecture-overview.md: frontend does not duplicate domain
  calculations).

## Data Model Notes

`application/WeeklyInsights.java` (record: `checkIn`, `main`, `secondary`,
`generatedAt`) is currently produced on demand by `WeeklyInsightsService`
with no persistence — verified against the source and against
`InsightsSection.tsx`'s doc comment, which explicitly documents both gaps
this story closes. New persistence needed: an insight-history table keyed
by period (week start date), storing enough of the `WeeklyInsights` shape to
reconstruct the history response. Delta computation needs access to the
prior period's persisted `checkIn` snapshot, so persisting `checkIn` (not
just the recommendations) is required.

## Edge Cases

- First-ever generated week (no prior period) → delta fields `null`, not
  zero or an error.
- History requested before any insights have ever been generated → empty
  list, not an error.
- A gap week (no check-in generated for an intermediate week) → delta
  compares against the most recent prior persisted period, not a
  fabricated intermediate one; document the comparison rule.

## Open Questions

- Retention: keep all history indefinitely for MVP, or cap it — recommend
  no cap for MVP (personal, low-volume data), revisit if storage becomes a
  concern.
- Whether persistence happens synchronously on generation or lazily —
  recommend synchronous (simplest, matches "generated on demand" pattern);
  document the final decision.
