# FOR-133 Test Plan

Strict TDD: failing tests first, then implement. Mock `apiClient` and `window.location` — never real navigation or backend. The frontend must never handle/log a token.

## Scope

The connect-redirect wiring, the `completeIntegrationCallback` client call, and the `/auth` route states. Backend is done (FOR-131/132) and out of scope.

## API Client Tests (`integrations.ts`)

- `connectIntegration('WITHINGS')` → `POST /api/v1/integrations/withings/connect`, returns `{ authorizationUrl }` (updated shape; the old status shape is gone).
- `completeIntegrationCallback('WITHINGS', code, state)` → `POST /api/v1/integrations/withings/callback` with body `{ code, state }`, returns the connection status.
- Error propagation from the callback (400 invalid state) surfaces as a rejected promise the caller can render.

## Integraciones connect handler

- Clicking "Conectar Withings" calls `connectIntegration`, then redirects the browser to the returned `authorizationUrl` (assert `window.location.assign` called with it; `window.location` mocked).
- Double-click guarded (no double connect / double redirect).
- Connect failure before redirect → error surfaced, no navigation.

## `/auth` route

- Mount with `?code=X&state=Y` → calls `completeIntegrationCallback('WITHINGS', 'X', 'Y')`; on success navigates to Integraciones and fires a success toast.
- Mount with missing/blank `code` or `state` → error state, NO backend call.
- Mount with no query at all (direct visit) → "nothing to complete" / redirect, no backend call.
- Backend 400 (invalid/expired state) → `ErrorState` with retry/back actions.
- Callback runs once on mount (guard double-invocation under effect re-run/strict mode).
- Accessibility: loading `role="status"`, error `role="alert"`; existing axe test extended.

## Edge Cases

- `code`/`state` never persisted (no localStorage write).
- Provider ERROR/NEEDS_REAUTH result → readable message.

## Fixtures

- Mocked `apiClient` returning `{ authorizationUrl }` for connect and a status for callback, plus an error case.
- Mocked `window.location.assign`.
- React Router test wrapper with the `/auth` route mounted and a query string.
