# FOR-51: Build MVP dashboard overview

Jira: https://dbhlab.atlassian.net/browse/FOR-51
Epic: FOR-47 UI & UX

## Summary

Build the dashboard overview — the daily entry point that answers "what should I
pay attention to today?" in under 10 seconds (docs/ui-guidelines.md). Composes
widgets from existing feature read models: body composition, weekly training
status, today's nutrition, shopping budget, latest insight and integration sync
status. Mockup: `docs/1-dashboard.png`. `DashboardPage.tsx` already exists;
this story builds it out to the mockup using available APIs.

## User/System Flow

1. User lands on `/` (Dashboard).
2. Widgets load from feature read models (body FOR-17, training FOR-26,
   nutrition FOR-32/33, shopping FOR-38/39, insight FOR-45).
3. Each widget links to its feature page; empty/loading/error states per FOR-60.

## Functional Requirements

- Compose the dashboard from independently reusable widgets (no domain
  calculations in UI):
  - **Body composition summary**: PESO, GRASA CORPORAL, MASA MUSCULAR, IMC metric
    cards with "vs semana pasada" deltas + sparkline (reuse `MetricCard`,
    `LineChart`).
  - **Weekly training status**: próximo entrenamiento + weekly completion.
  - **Today nutrition summary**: calories vs target, macro ring, water.
  - **Shopping budget summary**: PRESUPUESTO SEMANAL weekly + monthly estimate.
  - **Latest insight/recommendation**: the FOR-45 main recommendation (message +
    reason), calm copy.
  - **Integration sync status**: "Withings · Conectado" summary.
- Each widget: loading, empty (new user) and error states.
- Each widget navigates to its feature page.
- Header greeting ("Hola Diego 👋", "Este es tu resumen de hoy") + date.

## Non-Functional Requirements

- Clarity over density; restrained accent (docs/ui-guidelines.md).
- No business rules duplicated in components; read models only.
- Usable on mobile (mockup shows a Hoy/Semana/Mes phone layout).

## Data Model Notes

Consumes existing read models only. Dashboard widgets shown in
docs/ui-guidelines.md "Dashboard widgets". **Mockup extras not yet backed** (e.g.
"AGUA / hydration", "TENDENCIA 30 DÍAS", running RITMO/FRECUENCIA CARDÍACA):
render only what an existing API supports; otherwise show as a clearly-labelled
placeholder or omit — do not invent backend (repository priority, AGENTS.md).

## Edge Cases

- New user with no data → each widget shows its empty state, not errors.
- A single failing widget must not break the whole dashboard.
- Missing insight → insufficient-data recommendation from FOR-45.

## Open Questions

- Which widgets have live endpoints today vs need mocks/placeholders — audit
  during implementation and document per widget (body/training/nutrition/
  shopping/insight exist; hydration and 30-day trend may not).
- Mobile Hoy/Semana/Mes tabs — recommend "Hoy" only for the MVP unless the
  weekly/month data is already available; document.
