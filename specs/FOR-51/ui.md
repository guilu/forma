# FOR-51 UI Spec

## Screens

- Dashboard (`frontend/src/pages/DashboardPage.tsx`) at `/`. Mockup:
  `docs/1-dashboard.png` (desktop grid + mobile Hoy view).

## Components

- Header greeting + date range control.
- Body metric cards (PESO / GRASA / MASA MUSCULAR / IMC) with delta + sparkline
  (`MetricCard` + `LineChart`).
- Training widget: próximo entrenamiento + weekly completion ring.
- Nutrition widget: calories vs target, macro ring, (water if backed).
- Shopping budget card: weekly total + monthly estimate.
- Insight card: FOR-45 main recommendation (message + reason).
- Sync status chip: "Withings · Conectado".
- Reuse FOR-50 primitives (Card, Badge, chart container).

## States

- Loading: each widget shows a skeleton/spinner (FOR-60), no layout jump.
- Empty: new-user copy per widget ("Aún no hay mediciones", target 0, etc.).
- Error: per-widget recoverable error with retry (FOR-60); siblings unaffected.
- Success: populated widgets matching the mockup.

## Interactions

- Clicking a widget/its "ver más" navigates to the feature page.
- Date control (Domingo, 8 Junio 2025) — current week; prev/next only if backed.
- Mobile Hoy/Semana/Mes tabs — Hoy for MVP unless weekly/month data available.

## Accessibility

- Each widget is a labelled region/`<section>` with a heading.
- Metrics are text (screen-reader friendly); deltas describe direction, not just
  color. Calm copy, no gamification.

## Responsive Behavior

- Desktop: multi-column card grid (mockup left panel).
- Mobile: single-column stack; summary table + calories + macros first
  (docs/ui-guidelines.md mobile priorities); no horizontal scroll.
