# FOR-52 AI Context

## Story

FOR-52 — Create body composition screens
(https://dbhlab.atlassian.net/browse/FOR-52)

## Intent

Let users review body evolution without interpreting raw numbers. Success is a
Mediciones screen with latest metrics, a weight trend chart, a history table, a
validated manual-entry form, derived metrics and a manual-vs-imported indicator —
honest about smart-scale noise.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (no fake precision, cards for metrics, trends)
- `docs/2-mediciones.png` (mockup)
- `docs/api/body-measurements.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/`..`FOR-20/` (body domain, API, prior UI)
- Jira: https://dbhlab.atlassian.net/browse/FOR-52

## Domain Notes

- `frontend/src/pages/MeasurementsPage.tsx`, `components/MeasurementForm.tsx`,
  `api/bodyMeasurements.ts` already exist — extend to the mockup.
- Derived fat/lean mass come from the domain/API; never recompute in UI.
- Distinguish imported (Withings) vs manual measurements via the API's source.

## Architectural Constraints

- Consume FOR-17 read/create endpoints via `api/bodyMeasurements.ts`. No domain
  logic in the UI. Use FOR-50 primitives + `LineChart`/`MetricCard`.

## Common Pitfalls

- Recomputing derived metrics in the UI.
- Presenting smart-scale values as lab-grade precision.
- Breaking the chart/table on the empty (first-measurement) state.

## Suggested Implementation Order

1. Latest metric cards + deltas from the list endpoint.
2. Weight evolution chart with a range selector (capped to available data).
3. History table + "ver todas"; source indicator.
4. Manual entry form with inline validation; empty/loading/error states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/2-mediciones.png`.
