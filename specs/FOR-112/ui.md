# FOR-112 UI Spec

## Screens

- Cross-cutting — every screen that renders `Card`, `MetricCard` or
  `ChartContainer`: Dashboard, Mediciones, Entrenamiento, Nutrición, Lista
  de compra, Progreso, Ajustes, Integraciones.

## Components

- `Card` (`frontend/src/components/Card.tsx`) — gains `headingLevel` prop,
  default `3`.
- `MetricCard` (`frontend/src/components/MetricCard.tsx`) — forwards
  `headingLevel` to `Card`.
- `ChartContainer` (`frontend/src/components/ChartContainer.tsx`) —
  forwards `headingLevel` to `Card`.

## States

N/A — structural/semantic change only, no new visual states.

## Interactions

N/A — no new interaction; heading level does not affect focus order or
keyboard behavior.

## Accessibility

- Every screen's heading order becomes non-skipping: `<h1>` page title →
  `<h2>` section headings (where present) → `<h3>` (or the explicit
  `headingLevel` set per call site) card titles, with no level skipped.
- Verifiable via the accessibility tree or an axe scan (FOR-114) —
  "heading-order" and "page-has-heading-one" rules should pass on every
  audited screen.

## Responsive Behavior

None — heading level is independent of viewport size; visual size/weight of
card titles is unchanged regardless of the underlying tag.
