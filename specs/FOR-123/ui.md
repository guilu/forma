# FOR-123 UI Spec

## Screens

- Conexiones / Integraciones (`frontend/src/pages/integrations/
  IntegrationsSection.tsx`), at `/ajustes/integraciones` and embedded in
  Ajustes.

## Components

- `IntegrationsSection` — `handleConnect`, `handleSync`,
  `handleDisconnectConfirm` each gain a `notify.success(...)` call in
  their try branch, using the existing `useNotify()` hook (FOR-63).

## States

- Loading/error/success states at the section level unchanged. New:
  success toast fires on a successful connect/sync/disconnect, in addition
  to the existing behavior.

## Interactions

- Connect/Sync/Disconnect buttons — unchanged trigger; success now also
  produces a toast (previously unreachable given `Promise<never>`).

## Accessibility

- Success toast follows `NotificationProvider`'s existing `aria-live`
  announcement pattern (FOR-63) — no new accessibility work needed beyond
  what FOR-63 already built.

## Responsive Behavior

No change.
