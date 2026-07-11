# FOR-98: Expose weekly training summary over HTTP

Jira: https://dbhlab.atlassian.net/browse/FOR-98
Epic: FOR-95 UI Backend Enablers

## Summary

Expose the FOR-28 `WeeklyTrainingSummary` over HTTP so the UI reads an
authoritative weekly training summary (planned/completed running & strength
counts + planned/completed running distance) instead of tallying client-side.
Thin controller over the existing `WeeklyTrainingSummaryService` — no new domain.

## User/System Flow

1. Client calls `GET /api/v1/training/weekly-summary`.
2. The controller delegates to `WeeklyTrainingSummaryService.currentSummary()`
   (FOR-28) and maps the result to a response DTO.
3. The dashboard (FOR-51) / training screen (FOR-53) render the summary.

## Functional Requirements

- Add `GET /api/v1/training/weekly-summary` under `ApiPaths.V1`, mounted in a
  thin controller (extend `TrainingController` or a sibling), delegating to the
  existing `WeeklyTrainingSummaryService` (FOR-28). No business logic in the
  controller (ADR-001).
- Response carries the FOR-28 `WeeklyTrainingSummary` fields: planned/completed
  running sessions, planned/completed strength sessions, total planned running
  km, completed running km, and the message.
- Response DTO is distinct from the application `WeeklyTrainingSummary` type
  (ADR-005) — controllers never return application/domain types directly.

## Non-Functional Requirements

- Deterministic, computed on demand (consistent with FOR-28) — no persistence.
- No fake precision; carry km as the domain rounds them.

## Data Model Notes

Reuses `application/WeeklyTrainingSummary` (record: `plannedRunningSessions`,
`completedRunningSessions`, `plannedStrengthSessions`, `completedStrengthSessions`,
`totalPlannedRunningKm`, `completedRunningKm`, `message`) and
`WeeklyTrainingSummaryService.currentSummary()`. No new persisted entity.

## Edge Cases

- Empty week (nothing planned) → counts 0, km 0.0, non-alarming message; 200 OK.
- Current week only (no week parameter), consistent with FOR-26/FOR-28.

## Open Questions

- Endpoint placement: recommend `GET /api/v1/training/weekly-summary` on the
  existing `TrainingController` (alongside `/week`) — document.
- Whether to also expose duration/volume: out of scope; FOR-28 does not compute
  them. Defer.
