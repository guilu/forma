# FOR-97 AI Context

## Story

FOR-97 — Expose weekly body summary over HTTP
(https://dbhlab.atlassian.net/browse/FOR-97)

## Intent

Give the UI honest week-over-week body deltas over HTTP. Success is a thin
`GET /api/v1/body/weekly-summary` returning the FOR-21 summary via a delivery DTO.

## Relevant Documents

- `AGENTS.md`
- `docs/api/body-measurements.md`, `docs/api-conventions.md`
- `docs/adr/ADR-005-api-design.md`, `docs/adr/ADR-001-architecture.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-21/` (WeeklyBodySummary), `specs/FOR-15/`..`FOR-17/` (body domain/API)
- Jira: https://dbhlab.atlassian.net/browse/FOR-97

## Domain Notes

- `application/WeeklyBodySummaryService.currentSummary()` already returns a
  `domain/WeeklyBodySummary` — reuse it; do NOT recompute deltas.
- Mirror the existing `BodyMeasurementController` + `BodyMeasurementResponse.from`
  pattern for the controller + DTO (both in `delivery/body`).
- Honesty: deltas null when < 2 measurements; `comparisonDays` = real day gap.
- No HTTP summary endpoint exists yet (verified: nothing under `delivery/`).

## Architectural Constraints

- Thin controller under `delivery/body` on `ApiPaths.V1` (ADR-001). DTO distinct
  from the domain record (ADR-005). No persistence.

## Common Pitfalls

- Returning the domain `WeeklyBodySummary` directly from the controller.
- Presenting deltas as `0` instead of `null` when there is no prior measurement.
- Hardcoding the path without the `/api/v1` prefix.

## Suggested Implementation Order

1. `WeeklyBodySummaryResponse` DTO (`from(WeeklyBodySummary)`).
2. `GET /api/v1/body/weekly-summary` controller delegating to the service.
3. `@WebMvcTest` (with measurements, single measurement, and none).

## Validation

Run `./gradlew test spotlessApply` from `backend/`. Confirm the endpoint returns
latest values + deltas + `comparisonDays`, with nulls when data is insufficient.
