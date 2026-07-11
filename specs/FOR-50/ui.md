# FOR-50 UI Spec

## Screens

- No screen of its own — foundational primitives consumed by all screens. A
  lightweight usage/examples surface is acceptable.

## Components

- Tokens (`styles/theme.css`): color, typography, spacing, radius, elevation
  (exist; extend minimally).
- `Button` — variants: primary/accent, secondary (outline), ghost, destructive;
  sizes; disabled + loading.
- `Card` (exists) + section header pattern (title + optional action, as in the
  mockups' "EVOLUCIÓN DE PESO", "COMIDAS DEL DÍA" cards).
- Form field: input, select, inline error text (align with `MeasurementForm`).
- `Badge`/`StatusPill` — severity, connection status, plazo tags, "Saludable".
- Chart container wrapping `LineChart` for consistent framing.

## States

- Loading: button loading state; chart container loading frame.
- Empty: chart container empty frame.
- Error: input error style; destructive/danger button.
- Success: default rendered variants.

## Interactions

- Buttons: hover/active/focus/disabled per tokens; accent used sparingly for
  primary actions and active states only.
- Inputs: focus ring, error display near the field.

## Accessibility

- Native `<button>` / labelled inputs; visible focus from tokens; contrast meets
  the palette's high-contrast intent (verified fully in FOR-61).

## Responsive Behavior

- Primitives are fluid (relative units, `max-width: 100%`); cards reflow in grids
  defined by feature pages. Tokens include spacing scale for consistent rhythm
  across breakpoints.
