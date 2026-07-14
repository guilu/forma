# FOR-123 Test Plan

## Scope

Verify connect/sync/disconnect each show a success toast on a resolving
call, with no change to existing error handling.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — depends on FOR-103's backend; no backend change owned by this story.

## UI Tests

- A successful `connectIntegration` call triggers a success toast naming
  the provider.
- A successful `syncIntegration` call triggers a success toast naming the
  provider.
- A successful `disconnectIntegration` call (after confirm) triggers a
  success toast naming the provider.
- A failing call (any of the three) still renders the existing
  `actionError` message and does not also show a success toast.
- Rapid repeated sync clicks on the same provider do not stack multiple
  success toasts (`NotificationProvider` dedupe, FOR-63).

## Edge Cases

- Disconnect success toast fires after the confirm dialog has closed, not
  overlapping its dismissal animation/focus return.

## Fixtures

- Mocked `connectIntegration`/`syncIntegration`/`disconnectIntegration`
  that resolve successfully (once FOR-103's real return type exists) and
  mocked rejecting versions for the regression check.
