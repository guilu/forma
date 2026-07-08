# FOR-45 AI Context

## Story

FOR-45 — Expose weekly insights API
(https://dbhlab.atlassian.net/browse/FOR-45)

## Intent

Give the dashboard one endpoint for the week's status + guidance. Success is
`GET /api/v1/insights/weekly` returning the FOR-40 check-in, a prioritized main
recommendation, secondary ones, and a generated timestamp — degrading gracefully
on empty data.

## Relevant Documents

- `AGENTS.md`
- `docs/api-conventions.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `docs/api/body-measurements.md`, `docs/api/training-week.md` (endpoint + error
  precedents)
- `specs/FOR-40/`, `specs/FOR-41/`, `specs/FOR-42/`, `specs/FOR-43/`,
  `specs/FOR-44/`
- Jira: https://dbhlab.atlassian.net/browse/FOR-45

## Domain Notes

- Assemble the FOR-40 check-in (FOR-21 body + FOR-28 training), run the
  FOR-42/43/44 rules, pick a main recommendation by priority (`ACTION` >
  `WARNING` > `INFO`), and return with a generated timestamp.
- Computed on demand; no persisted insights.

## Architectural Constraints

- Thin controller under `delivery/` mounted on `ApiPaths.V1`; DTOs distinct from
  domain (ADR-005). Reuse the existing error handler (FOR-88/FOR-27).
- An application service orchestrates check-in assembly + rule evaluation +
  main-selection (FOR-21/FOR-28 service pattern).

## Common Pitfalls

- Returning domain types directly from the controller.
- Erroring on empty data instead of an insufficient-data response.
- Hardcoding `/api/insights` without the `/api/v1` prefix.
- Non-deterministic main selection (define a stable priority).

## Suggested Implementation Order

1. Application service: assemble check-in, run rules, select main by priority.
2. Handle the empty-data path (insufficient-data main, no secondaries).
3. Thin controller + response DTO (`api.md`).
4. Controller tests (`@WebMvcTest`): populated week + empty week.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend" row).
