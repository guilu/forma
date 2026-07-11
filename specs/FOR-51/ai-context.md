# FOR-51 AI Context

## Story

FOR-51 — Build MVP dashboard overview
(https://dbhlab.atlassian.net/browse/FOR-51)

## Intent

The dashboard is the daily entry point: surface the week's status + one clear
recommendation fast. Success is a composed, widget-based overview reading only
existing feature read models, with proper empty/loading/error states and links to
each feature page.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` ("answer weekly status in <10s", dashboard widgets,
  interaction style)
- `docs/1-dashboard.png` (mockup — desktop + mobile)
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-001-architecture.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-45/` (weekly insights), `specs/FOR-21/`/`FOR-28/` (summaries),
  `specs/FOR-38/`/`FOR-39/` (budget/list)
- Jira: https://dbhlab.atlassian.net/browse/FOR-51

## Domain Notes

- `frontend/src/pages/DashboardPage.tsx` exists — build it out, don't recreate.
- Available data: body measurements (FOR-17), training week (FOR-26/27),
  nutrition day (FOR-32/33), shopping budget/list (FOR-38/39), weekly insights
  (FOR-45). Reuse `MetricCard`, `LineChart`, `Card`.
- Mockup shows richer widgets (hydration, 30-day trend, running HR/pace) that may
  exceed current backend — surface only what an API supports.

## Architectural Constraints

- Widgets are reusable presentational components fed by read models; no domain
  calculations in the UI (ADR-001). Use design-system primitives (FOR-50).

## Common Pitfalls

- Duplicating domain calculations in widgets.
- One widget's failure taking down the page (isolate states).
- Faking data for widgets with no backend instead of a labelled placeholder.

## Suggested Implementation Order

1. Map each widget to an existing read model / API client in `frontend/src/api`.
2. Build body, training, nutrition, shopping, insight, sync widgets with
   loading/empty/error states (FOR-60).
3. Wire per-widget navigation to feature pages.
4. Responsive layout (desktop grid + mobile stack); render-state tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare desktop + mobile against `docs/1-dashboard.png`.
