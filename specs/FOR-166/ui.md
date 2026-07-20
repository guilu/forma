# FOR-166 UI Spec

## Screens

- Mediciones (`frontend/src/pages/MeasurementsPage.tsx`), route `/mediciones`. Template: `docs/2-mediciones.html`.

## Components

- `MetricCard` (latest metrics), history list, `LineChart`/`ChartContainer` (trend), `MeasurementForm`
  (manual entry) — restyled via FOR-164 + FOR-163 tokens.

## States

- Latest card, history list, chart, form — success state to the template.
- Empty (before first measurement) → `EmptyState`; loading → `LoadingState`; error → `ErrorState` (FOR-60).
- Validation errors adjacent to fields.

## Interactions

- Manual entry flow (open form → validate → submit) unchanged in behaviour.
- Manual vs imported indicator visible on each value.

## Accessibility

- Form labels + associated errors; manual/imported conveyed by text/icon not color alone; contrast both themes.

## Responsive Behavior

- Mobile single-column: cards stack, history list/table scrolls in its own container, form full-width.
