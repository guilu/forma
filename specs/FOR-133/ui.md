# FOR-133 UI Spec

## Screens

- **Integraciones** (`IntegrationsSection`, existing) — the "Conectar {provider}" button now starts an OAuth redirect.
- **`/auth`** (new route/page) — the OAuth callback landing that completes the connection.

## Components

- Reuse: shared `LoadingState` / `ErrorState` / `EmptyState` (FOR-60), `Button`, `useNotify` toasts (FOR-63), `Card` with `headingLevel` (FOR-112).
- New: an `AuthCallbackPage` (or similar) mounted at `/auth`.

## States

### Integraciones connect action
- Idle → user clicks "Conectar Withings".
- Pending: button `loading` while `POST /connect` is in flight.
- On response: full-page navigation to the Withings `authorizationUrl` (the app briefly leaves — expected).
- On connect failure (before redirect): inline/toast error, stays on Integraciones.

### `/auth` route
- **Loading**: "Completando conexión con Withings…" while `POST /callback` runs (`LoadingState`).
- **Success**: brief confirmation, then navigate to Integraciones with a success toast ("Conectado con Withings").
- **Error**: `ErrorState` with a readable message and actions:
  - "Volver a intentar" → re-initiates connect (or returns to Integraciones to retry).
  - "Volver a Integraciones" → navigate back.
- **Nothing to complete** (no `code`/`state`): friendly message or immediate redirect to Integraciones.

## Interactions

- Connect button: single click → `POST /connect` → redirect. Prevent double-submit while pending.
- `/auth`: runs the callback once on mount (guard against double-invocation, e.g. React strict-mode/effect re-run).
- Error retry re-initiates the OAuth flow cleanly (fresh state).

## Accessibility

- `/auth` has a page-level heading; states are announced (`role="status"` for loading/success, `role="alert"` for error), consistent with FOR-60.
- Keyboard-operable actions; result not conveyed by color alone.
- The connect redirect is triggered by a real button, not an unlabeled control.

## Responsive Behavior

- `/auth` is a simple centered status page — works at all widths.
- Integraciones unchanged beyond the connect button behavior.
