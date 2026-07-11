# FOR-52 UI Spec

## Screens

- Mediciones (`frontend/src/pages/MeasurementsPage.tsx`) at `/mediciones`.
  Mockup: `docs/2-mediciones.png` (Resumen / Evolución / Historial on mobile).

## Components

- Latest metric cards (PESO/GRASA/MASA/IMC/AGUA) with delta + sparkline.
- Weight evolution chart (`LineChart`) + range selector (7D/1M/3M/6M/1A/Todo).
- "Últimas mediciones" table + "Ver todas".
- Manual entry: `MeasurementForm` (in `Modal` or a route) with inline validation.
- Source badge (manual vs Withings).
- Optional body-distribution panel (only if backed) — else deferred placeholder.

## States

- Loading: cards/chart/table skeletons (FOR-60).
- Empty: no measurements → "Aún no hay mediciones" + "Registrar medición" CTA.
- Error: load failure → error + retry; submit failure → inline error, list kept.
- Success: populated cards, chart, table; new entry appears after save.

## Interactions

- "Registrar medición" opens the manual entry form; submit persists via
  `api/bodyMeasurements` and refreshes.
- Range selector changes the chart window (capped to available data).
- "Ver todas las mediciones" expands/navigates to full history.

## Accessibility

- Form fields labelled; errors associated with inputs and announced.
- Metrics/deltas are text; direction conveyed by words + icon, not color alone.
- Table has header cells; chart has an accessible summary.

## Responsive Behavior

- Desktop: card row + chart + table/distribution two-column.
- Mobile: Resumen/Evolución/Historial tabs; cards stack; table scrolls within its
  own container; "Add measurement" easy to reach (mobile priority).
