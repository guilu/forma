# FOR-97: Expose weekly body summary over HTTP

Jira: https://dbhlab.atlassian.net/browse/FOR-97
Epic: FOR-95 UI Backend Enablers

## Summary

Expose the FOR-21 `WeeklyBodySummary` over HTTP so the UI can show honest
week-over-week body deltas (weight / body fat) alongside the latest values. Thin
controller over the existing `WeeklyBodySummaryService` — no new domain,
computed on demand.

## User/System Flow

1. Client calls `GET /api/v1/body/weekly-summary`.
2. The controller delegates to `WeeklyBodySummaryService.currentSummary()`
   (FOR-21) and maps the result to a response DTO.
3. The dashboard (FOR-51) / Mediciones (FOR-52) render latest values + "vs
   semana pasada" deltas.

## Functional Requirements

- Add `GET /api/v1/body/weekly-summary` under `ApiPaths.V1`, in a thin controller
  (a sibling in `delivery/body`), delegating to `WeeklyBodySummaryService`
  (FOR-21). No business logic in the controller (ADR-001).
- Response carries the FOR-21 `WeeklyBodySummary` fields: latest weight / body-fat
  / lean-mass, weekly weight & body-fat deltas, `comparisonDays`, and the message.
- Response DTO is distinct from the domain `WeeklyBodySummary` (ADR-005).

## Non-Functional Requirements

- Deterministic, computed on demand (consistent with FOR-21) — no persistence.
- Honesty rules from FOR-21: deltas are `null` (not `0`) with fewer than two
  measurements; `comparisonDays` states the actual gap (no fake "one week").

## Data Model Notes

Reuses `domain/WeeklyBodySummary` (record: `latestWeightKg`,
`latestBodyFatPercentage`, `latestLeanMassKg`, `weeklyWeightChangeKg`,
`weeklyBodyFatChange`, `comparisonDays`, `message`) and
`WeeklyBodySummaryService.currentSummary()`. No new persisted entity.

## Edge Cases

- No measurements → latest values null, deltas null, informative message; 200 OK.
- Exactly one measurement → latest values present, deltas + `comparisonDays`
  null.
- Gap longer than a week → `comparisonDays` reflects the real number of days.

## Open Questions

- Endpoint placement: recommend `GET /api/v1/body/weekly-summary` (sibling of
  `/body/measurements`) — document.
- Whether to expose the FOR-101 `bmiCategory` here too — out of scope for this
  story; keep the summary to FOR-21's fields and let FOR-101 own the category.
