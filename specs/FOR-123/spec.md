# FOR-123: Integrations: success feedback on connect/sync/disconnect

Jira: https://dbhlab.atlassian.net/browse/FOR-123
Epic: FOR-47 UI & UX

## Summary

`IntegrationsSection.tsx`'s `handleConnect`/`handleSync`/
`handleDisconnectConfirm` (lines 114–152) each `await` their respective API
call and handle the `catch` branch (setting `actionError`), but none call
`useNotify().success()` on the try branch — because `connectIntegration`,
`syncIntegration` and `disconnectIntegration`
(`frontend/src/api/integrations.ts`) are typed `Promise<never>` and always
reject today (no backend, FOR-103 not yet built), so a success branch would
be unreachable dead code. The component's own doc comment says exactly
this: "Wiring `useNotify()` success feedback here is follow-up work for
whenever FOR-103 lands." This story is that follow-up.

## User/System Flow

1. FOR-103 ships a real integrations backend; `connectIntegration`/
   `syncIntegration`/`disconnectIntegration` change from always-rejecting
   `Promise<never>` to real request functions that can resolve.
2. User connects/syncs/disconnects a provider; on success, a toast confirms
   the action (FOR-63 pattern, same as `ShoppingPage.toggle()`'s existing
   `notify.success('Artículo actualizado.')` precedent).
3. Failure path is unchanged — `actionError` continues to render the
   existing inline error message.

## Functional Requirements

- Once `connectIntegration`/`syncIntegration`/`disconnectIntegration`'s
  return types change from `Promise<never>` to a real resolving type
  (FOR-103's responsibility, not this story's), add a `notify.success(...)`
  call to each handler's try branch: `handleConnect`,
  `handleSync`, `handleDisconnectConfirm`.
- Success messages should be specific and calm (ui-guidelines.md), e.g.
  "Conectado con {providerName}.", "Sincronizado con {providerName}.",
  "Desconectado de {providerName}." — mirroring the specificity of the
  existing error messages in the same handlers.
- No change to the existing error-handling branches, `pendingProviderId`
  loading-state pattern, or the disconnect confirmation dialog (FOR-63) —
  this story only adds the previously-unreachable success branch.
- `useNotify()` import/usage follows the exact pattern already established
  in `ShoppingPage.tsx` (`const notify = useNotify();` at the top of the
  component).

## Non-Functional Requirements

- No premature wiring against a backend that doesn't exist — this story
  only makes sense once FOR-103 ships; implementing it earlier would mean
  writing success-feedback code with no way to verify it fires (the
  functions structurally cannot resolve until FOR-103 changes their return
  type).
- Consistent, non-gamified copy (docs/ui-guidelines.md, same standard
  FOR-63 established).

## Data Model Notes

None — purely a UI feedback wiring change. No new state beyond the
existing `actionError`/`pendingProviderId`.

## Edge Cases

- Rapid repeated sync clicks → existing `pendingProviderId` guard already
  prevents overlapping calls; success toast follows the same de-dupe
  behavior `NotificationProvider` already provides for `ShoppingPage`
  (FOR-63 spec edge case: "avoid stacking excessive notifications").
- Disconnect success → toast fires after the confirm dialog closes
  (`setDisconnectTarget(undefined)` already happens in `finally`), not
  overlapping with the dialog's own dismissal.

## Open Questions

- None beyond the hard blocker: this story cannot be meaningfully
  implemented or tested until FOR-103 changes `connectIntegration`/
  `syncIntegration`/`disconnectIntegration` to resolve on success.
