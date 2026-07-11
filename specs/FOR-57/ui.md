# FOR-57 UI Spec

## Screens

- Integraciones — a section within Ajustes (`/ajustes`) or a sub-route. Mockup:
  "CONEXIONES E INTEGRACIONES" in `docs/8-configuracion.png`.

## Components

- Provider row: icon, name, description, status pill ("Conectado" / "No
  conectado"), chevron/action.
- Connected-provider detail: last-sync timestamp, sync-now action, disconnect.
- Available-provider row: connect action.
- Reuse FOR-50 status/connection badges + `Card`.

## States

- Loading: provider list skeleton (FOR-60).
- Empty: no connected providers → available list + empty connected state.
- Error: connection/sync error → clear message, no sensitive data.
- Success: connected + available providers with status + last sync.

## Interactions

- Connect → hands off to OAuth (redirect handled elsewhere; entry point only).
- Disconnect → explicit confirmation (FOR-63 destructive pattern).
- Manual sync → triggers sync where supported; shows in-progress feedback.

## Accessibility

- Status conveyed by text + badge, not color alone.
- Provider rows/actions keyboard-operable with visible focus and labels.

## Responsive Behavior

- Desktop: provider cards in the settings layout.
- Mobile: stacked provider rows with chevrons (matches the phone mockup); no
  horizontal scroll.
