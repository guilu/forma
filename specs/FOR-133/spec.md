# FOR-133 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-133
Epic: FOR-47 UI & UX
Related: completes the Withings OAuth vertical (FOR-131 backend); fixes FOR-123's connect drift.

## Summary

Wire the Withings OAuth flow end-to-end in the SPA: the "Conectar" action redirects
the browser to the Withings authorization URL returned by the backend, and a new
`/auth` route receives the Withings redirect (`?code&state`), relays them to the backend
callback, and lands the user back on Integraciones with a clear result. Also fixes the
frontend/backend drift from FOR-131 (backend `connect` now returns an authorization URL,
not a status). Frontend-only.

## Repository baseline / drift

- FOR-123 wired `frontend/src/api/integrations.ts` `connectIntegration` against FOR-126's mock connect, returning a `ConnectionActionResult` (status/connectedAt). **This is now stale** — FOR-131's backend `POST /{provider}/connect` returns `{ authorizationUrl }`, and a new `POST /{provider}/callback` completes the flow.
- Router: `App.tsx` uses `useRoutes(routes)`; routes are defined in a module — add `/auth` there.
- Registered Withings redirect URL (FOR-131) is the SPA route `https://forma.diegobarrioh.dev/auth`.

## User/System Flow

1. User on Integraciones taps "Conectar Withings" → frontend calls `POST /api/v1/integrations/withings/connect` → receives `{ authorizationUrl }`.
2. Frontend redirects the browser to `authorizationUrl` (`window.location.assign`).
3. User authorizes at Withings → Withings redirects the browser to `https://forma.diegobarrioh.dev/auth?code=...&state=...`.
4. The `/auth` route reads `code`+`state`, calls `POST /api/v1/integrations/withings/callback` with `{ code, state }`.
5. On success → navigate to Integraciones with a success toast (provider now CONNECTED). On error → readable error state with a way back.

## Functional Requirements

- `integrations.ts`: `connectIntegration(provider)` now returns `{ authorizationUrl }`; add `completeIntegrationCallback(provider, code, state)` → `POST /{provider}/callback` `{ code, state }` returning the connection status.
- Integraciones connect handler: call connect, then `window.location.assign(authorizationUrl)`. Disconnect/sync unchanged.
- New `/auth` route + page: read `code`+`state` from the query; if both present, call the callback; success → navigate to Integraciones + success toast; error → error state (missing params, backend 400 invalid/expired state, provider error) with retry/back.
- Register `/auth` in the app router.
- The frontend handles only `code`/`state` in transit — never a token; never persist them.

## Non-Functional Requirements

- Accessible loading/success/error states (FOR-60 shared components), `useNotify` (FOR-63).
- No secret/token in the frontend; `code`/`state` are single-use and not stored.
- Token-driven styling.

## UI / States (see ui.md)

- `/auth`: loading (calling callback), success (brief, then redirect), error (with retry/back to Integraciones).
- Integraciones: connect button triggers a full-page redirect (document the momentary navigation away).

## Edge Cases

- `/auth` opened with no `code`/`state` (direct visit) → friendly "nothing to complete" / redirect to Integraciones; no backend call with junk.
- Missing or blank `code` or `state` → error, no callback call.
- Backend 400 (invalid/expired/replayed state) → readable error + "Volver a intentar" (re-initiates connect) / back to Integraciones.
- Provider ERROR / NEEDS_REAUTH surfaced by the callback → readable message.
- User navigates away mid-flow → no persisted partial state.

## Open Questions

- Provider is Withings-only for this story; keep the `/auth` route provider-aware or Withings-specific? Default: read the provider from state/param or default to Withings, documented.
- After success, where exactly to land (Integraciones section vs a dedicated confirmation) — default Integraciones with a success toast.
- Whether to auto-trigger a first sync after connect, or leave it to the user's manual sync — default manual (out of scope to auto-sync here).
