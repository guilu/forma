# FOR-133 AI Context

## Story

FOR-133 — Withings OAuth frontend: connect redirect + `/auth` callback route. Frontend-only. Completes the Withings OAuth vertical (FOR-131 backend) and fixes FOR-123's connect drift.

## Intent

Let the user actually authorize their Withings scale from the SPA: "Conectar" redirects to Withings; the `/auth` route completes the OAuth handshake against the backend. Success = a real end-to-end Withings connection (then the user can sync — FOR-132).

## Relevant Documents

- `specs/FOR-131/` (OAuth backend: `POST /connect` → `{authorizationUrl}`; `POST /callback` `{code,state}`), `specs/FOR-132/` (sync), `specs/FOR-123/` (the connect wiring being corrected).
- `AGENTS.md` — frontend consumes read models/commands; never handle secrets.
- `docs/adr/ADR-006-frontend.md`, `ADR-005-api-design.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-133

## Repo Notes (verified)

- `frontend/src/api/integrations.ts`: `connectIntegration` currently returns `ConnectionActionResult` (status/connectedAt) — the STALE FOR-126 shape. Backend now returns `{ authorizationUrl }`. Update it; add `completeIntegrationCallback`.
- `frontend/src/pages/integrations/IntegrationsSection.tsx`: the connect handler (from FOR-123, uses `useNotify`) must change to redirect to the authorization URL.
- Router: `App.tsx` uses `useRoutes(routes)` — routes live in a module; add `/auth` there.
- Registered Withings redirect (FOR-131): SPA route `https://forma.diegobarrioh.dev/auth`.
- Backend callback endpoint: `POST /api/v1/integrations/withings/callback` with `{ code, state }`.
- Reuse shared state components (FOR-60), `useNotify` (FOR-63), `Card` `headingLevel` (FOR-112).

## Architectural Constraints

- Frontend-only; consume the backend endpoints, no business logic.
- The SPA handles only `code`/`state` in transit — never a token; never persist them (no localStorage).
- Redirect to the authorization URL via `window.location.assign` (a real browser navigation, mockable in tests).
- Accessible states; no color-only meaning.

## Common Pitfalls

- Leaving `connectIntegration` on the old status shape (the drift) — it must return `{ authorizationUrl }`.
- Calling the backend callback with missing/blank `code`/`state` (direct `/auth` visit).
- Running the callback twice on mount (effect re-run / strict mode) — guard it.
- Persisting `code`/`state` (they are single-use secrets-in-transit).
- Trying to test real navigation/backend — mock `window.location` + `apiClient`.

## Suggested Implementation Order

1. `integrations.ts`: update `connectIntegration` return type to `{ authorizationUrl }`; add `completeIntegrationCallback(provider, code, state)` (+ client tests).
2. Integraciones connect handler: connect → `window.location.assign(authorizationUrl)` (+ test with mocked location).
3. `AuthCallbackPage` at `/auth`: read query, call callback once, success→navigate+toast, error states (+ tests).
4. Register `/auth` in the router.

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm: connect redirects to the authorization URL; `/auth` completes the callback and lands on Integraciones with a success toast; missing params / 400 → readable error, no junk backend call; no token handled/persisted; callback runs once. Live Withings E2E needs real credentials — out of automated scope.
