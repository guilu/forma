# FOR-56 AI Context

## Story

FOR-56 — Create insights and recommendations screens
(https://dbhlab.atlassian.net/browse/FOR-56)

## Intent

Show recommendations that are easy to understand, explainable and grounded in
recent data. Success is a calm insight surface reading FOR-45: current
recommendation + reason + related signals + severity + disclaimer.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (example insight copy, "always explain
  recommendations", no gamification)
- `docs/6-progreso.png` (related progress screen), `docs/api/weekly-insights.md`
- `docs/adr/ADR-005-api-design.md`, `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-40/`..`FOR-45/` (Insights engine + endpoint)
- Jira: https://dbhlab.atlassian.net/browse/FOR-56

## Domain Notes

- The only insights source is `GET /api/v1/insights/weekly` (FOR-45): `main` +
  `secondary` recommendations, each with `message`, `severity`, `reason`,
  optional `relatedMetric`, plus the `checkIn` and `generatedAt`.
- FOR-45 is computed-on-demand — no persisted history yet.
- Reuse the dashboard insight widget (FOR-51) so copy/severity styling is shared.

## Architectural Constraints

- Consume FOR-45 via a `frontend/src/api/insights.ts` client (add if missing). No
  rule logic or rationale synthesis in the UI. Reuse FOR-50 severity badges.

## Common Pitfalls

- Inventing explanation text instead of rendering the backend `reason`.
- Alarming/gamified copy or medical-sounding claims.
- Building a history list with no backend to feed it.

## Suggested Implementation Order

1. Add/verify an insights API client for FOR-45.
2. Current insight card (message + severity + reason) + related signals from the
   check-in.
3. Secondary recommendations; disclaimer.
4. Empty/error states; defer history; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Verify copy tone against docs/ui-guidelines.md examples.
