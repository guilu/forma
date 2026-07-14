# FOR-115 UI Spec

## Screens

- Configuración / Ajustes (`frontend/src/pages/SettingsPage.tsx`) at
  `/ajustes`. Mockup: `docs/8-configuracion.png`, "Soporte y ayuda" block.

## Components

- New `SupportSection` (`frontend/src/pages/settings/SupportSection.tsx`) —
  a `Card` with title "Soporte y ayuda", composed of `SettingsRow` entries
  (reusing `frontend/src/pages/settings/SettingsRow.tsx`), mirroring
  `AboutSection.tsx`'s structure exactly.

## States

- Loading: N/A — static content, no async load (same as `AboutSection`).
- Empty: N/A.
- Error: N/A — no data fetch to fail.
- Success: section renders with its entries, inert where no backend/page
  exists.

## Interactions

- Working entries (if any, e.g. an external help-center link) navigate/open
  as a normal link.
- Inert entries render as disabled/non-interactive rows, matching
  `AboutSection`'s "Términos y condiciones" / "Política de privacidad"
  precedent.

## Accessibility

- `SettingsRow`'s existing labelled-row semantics reused as-is; inert rows
  marked as such (not focusable as if they were active controls), same
  contract as the existing `inert` prop.

## Responsive Behavior

- Follows `SettingsPage`'s existing CSS-grid, mobile-first pattern: stacks
  into the same single-column scrollable list on narrow viewports as every
  other Ajustes section.
