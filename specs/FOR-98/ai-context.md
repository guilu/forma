# FOR-98 AI Context

## Story

FOR-98 — Expose weekly training summary over HTTP
(https://dbhlab.atlassian.net/browse/FOR-98)

## Intent

Give the UI an authoritative weekly training summary over HTTP instead of
client-side tallying. Success is a thin `GET /api/v1/training/weekly-summary`
returning the FOR-28 summary via a delivery DTO.

## Relevant Documents

- `AGENTS.md`
- `docs/api/training-week.md`, `docs/api-conventions.md`
- `docs/adr/ADR-005-api-design.md`, `docs/adr/ADR-001-architecture.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-28/` (WeeklyTrainingSummary), `specs/FOR-26/`/`FOR-27/` (week/status)
- Jira: https://dbhlab.atlassian.net/browse/FOR-98

## Domain Notes

- `application/WeeklyTrainingSummaryService.currentSummary()` already returns a
  `WeeklyTrainingSummary` record — reuse it; do NOT recompute.
- Mirror the existing `TrainingController` + `TrainingWeekResponse.from(...)`
  pattern for the controller + DTO.
- No HTTP summary endpoint exists yet (verified: nothing under `delivery/`).

## Architectural Constraints

- Thin controller under `delivery/training` on `ApiPaths.V1` (ADR-001). DTO in
  `delivery/training`, distinct from the application record (ADR-005). No
  persistence.

## Common Pitfalls

- Returning the application `WeeklyTrainingSummary` directly from the controller.
- Recomputing counts/km in the controller instead of delegating to the service.
- Hardcoding the path without the `/api/v1` prefix.

## Suggested Implementation Order

1. Add a `WeeklyTrainingSummaryResponse` DTO (`from(WeeklyTrainingSummary)`).
2. Add `GET /weekly-summary` to `TrainingController` delegating to the service.
3. `@WebMvcTest` for the endpoint (populated + empty week).

## Validation

Run `./gradlew test spotlessApply` from `backend/`. Confirm
`GET /api/v1/training/weekly-summary` returns the expected shape.
