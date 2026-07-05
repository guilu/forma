# FOR-19 AI Context

## Story

FOR-19 — Create body dashboard metric cards
(https://dbhlab.atlassian.net/browse/FOR-19)

## Intent

Turn the dashboard's current "no data sources yet" placeholder into real
metric cards backed by the FOR-17 API, matching the widgets already
described in docs/ui-guidelines.md. Success is correct latest-measurement
display with a clean empty state — no fake data, no fake precision.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`
- `docs/adr/ADR-006-frontend.md`
- `specs/FOR-17/` (API this dashboard reads)
- Jira: https://dbhlab.atlassian.net/browse/FOR-19

## Domain Notes

All five metrics come from one `BodyMeasurement` (the latest one). No new
domain concepts are introduced.

## Architectural Constraints

- Replace `frontend/src/pages/DashboardPage.tsx` (currently a
  `PagePlaceholder` with description "El resumen diario se construirá cuando
  existan sus fuentes de datos" — this story is that data source).
- Reuse `frontend/src/components/Card.tsx`.
- Call the API only through `frontend/src/api/client.ts`.
- Do not recompute fat mass/lean mass client-side (ADR-006).

## Common Pitfalls

- Rendering `0` or `NaN` cards instead of a real empty state when there are
  no measurements.
- Showing more decimal precision than the input actually has
  (docs/ui-guidelines.md explicitly calls out "no fake precision").
- Adding a new backend endpoint before confirming FOR-17's existing `GET`
  list endpoint is actually insufficient (see spec.md Open Questions).

## Suggested Implementation Order

1. Fetch the measurement list via `apiClient` and take the latest entry.
2. Render the five metric cards from that entry.
3. Implement the empty state for zero measurements.
4. Verify rounding/precision rules against docs/ui-guidelines.md.

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`, per AGENTS.md Verification guidance "Frontend" row).
