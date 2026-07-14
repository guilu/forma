# FOR-116 UI Spec

## Screens

- Conexiones / Integraciones (`frontend/src/pages/integrations/
  IntegrationsSection.tsx`), mounted at `/ajustes/integraciones` and
  embedded in Ajustes. Mockup reference: "CONEXIONES E INTEGRACIONES" in
  `docs/8-configuracion.png`.

## Components

- `IntegrationsSection` — `PROVIDER_ICONS` mapping (or its replacement)
  swapped from generic `Icon` names to brand logo assets; both render
  sites (connected list, available-providers list) updated.

## States

- Loading / Empty / Error / Success: unchanged — this story only affects
  the icon rendered within the existing states, not the states themselves.

## Interactions

- Unchanged — connect/sync/disconnect entry points, confirm dialog and
  (currently non-functional, per FOR-123) success feedback are untouched.

## Accessibility

- Each brand logo keeps (or gains, if missing today) an accessible name
  via the existing `Icon`-equivalent pattern (`aria-label` or adjacent
  visible provider name text) — a logo alone must not be the only
  identification of the provider for screen reader users.
- Contrast/visibility verified in both light and dark themes (FOR-62).

## Responsive Behavior

No change — logos render at the same size/slot the generic icons occupied
today; no new breakpoints introduced.
