# FOR-45: Expose weekly insights API

Jira: https://dbhlab.atlassian.net/browse/FOR-45
Epic: FOR-6 Insights Engine

## Summary

Expose a single weekly insights endpoint: the FOR-40 check-in summary plus the
main and secondary recommendations (FOR-42/43/44) and a generated timestamp, so
the dashboard has one source for weekly status + guidance. Combines Body + Training
first; nutrition/shopping later. Handles empty data gracefully.

## User/System Flow

1. Client calls `GET /api/v1/insights/weekly`.
2. The application assembles the FOR-40 `WeeklyCheckIn`, runs the FOR-42/43/44
   rules to produce recommendations, picks a main one, and returns them with a
   generated timestamp.
3. The dashboard renders the summary + recommendations.

## Functional Requirements

- Endpoint `GET /api/v1/insights/weekly` under `ApiPaths.V1` (Jira writes
  `/api/insights/weekly`; apply the established `/api/v1` prefix — documented
  adaptation, as in FOR-17).
- Response includes: the weekly check-in summary (FOR-40), a **main**
  recommendation, **secondary** recommendations when available, and a
  **generated timestamp**.
- Combine Body (FOR-21) and Training (FOR-28) data first; nutrition/shopping may
  be added later.
- Handle empty data gracefully — return a valid response (e.g. an
  insufficient-data recommendation), not an error.
- Controller stays thin (ADR-001); DTOs distinct from domain types (ADR-005).

## Non-Functional Requirements

- Deterministic for the same underlying data; computed on demand (no persisted
  insights, consistent with FOR-21/FOR-28).
- No fake precision; neutral copy (docs/ui-guidelines.md).

## Data Model Notes

Aggregates FOR-40 `WeeklyCheckIn` + FOR-41 `Recommendation`s from the
FOR-42/43/44 rules. Choosing the "main" recommendation needs a priority (e.g.
`ACTION` > `WARNING` > `INFO`) — document the ordering. No new persisted entity.

## Edge Cases

- No data at all → a valid response with an insufficient-data recommendation as
  the main one; empty secondaries.
- Only body or only training data → partial check-in + whichever recommendations
  apply.
- Ties in recommendation priority — decide a stable ordering; document.

## Open Questions

- **Main-recommendation selection**: priority by severity (`ACTION` > `WARNING` >
  `INFO`), then category order — pick and document; Jira leaves it open.
- Whether the endpoint takes a week parameter or always the current week —
  recommend current week only for the MVP (consistent with FOR-28 scheduling).
