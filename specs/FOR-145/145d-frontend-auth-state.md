# FOR-145d Frontend Auth State

## Slice intent

Implement the frontend slice that turns the existing authenticated backend into a usable browser session flow for FORMA. This slice covers session bootstrap, login/register/logout UX, route protection, and the SPA-side CSRF/session mechanics required by ADR-012.

This slice does not add backend production behavior. It consumes the already-merged backend contract from FOR-145a and the owner-scoped backend work from FOR-145b-1 and FOR-145b-2.

## Repository reality

- `frontend/src/api/client.ts` currently calls `fetch` without `credentials` and never sends `X-XSRF-TOKEN`.
- `frontend/src/layout/Topbar.tsx` still renders a static `Diego` account area and comments that auth does not exist yet.
- The backend already exposes:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
  - `POST /api/v1/auth/logout`
- `SecurityConfig` requires cookie-based CSRF for unsafe methods and permits unauthenticated `GET /actuator/health`.

## Goals

1. Add frontend session state that can bootstrap the current user from the server cookie session.
2. Add public login and registration routes.
3. Protect the existing application shell and feature routes behind authentication.
4. Make the centralized API client session-cookie and CSRF aware.
5. Replace the static Topbar identity display with real session state and logout.
6. Add focused frontend tests for auth state, routing, and API client behavior.

## Backend contract consumed by this slice

### Auth user payload

`register`, `login`, and `me` return:

```json
{
  "id": "uuid",
  "email": "user@example.com"
}
```

The frontend must treat `email` as the only guaranteed display field in this slice. No profile name, avatar URL, refresh token, or role data exists here.

### Auth error model

Non-2xx responses use the existing `ApiError` shape already represented by `ApiRequestError` in `frontend/src/api/client.ts`.

### Session + CSRF model

- Session auth is carried by `JSESSIONID` cookie.
- CSRF uses `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` request header.
- Unsafe methods are `POST`, `PUT`, `PATCH`, and `DELETE`.
- `GET /actuator/health` is currently the safe unauthenticated priming request for obtaining the first `XSRF-TOKEN` before `register` or `login`.
- `GET /api/v1/auth/me` is the authenticated bootstrap/read endpoint after a session already exists.

## Functional scope

### 1. Auth state model

Add a frontend auth-state boundary that exposes:

- `status`: `loading | authenticated | anonymous`
- `user`: `{ id: string; email: string } | null`
- `login(credentials)`
- `register(credentials)`
- `logout()`
- `refreshCurrentUser()`

Behavior:

- On app bootstrap, the auth boundary requests `GET /api/v1/auth/me` exactly once.
- `200` sets `status=authenticated` and stores the returned user.
- `401` sets `status=anonymous` without showing a generic error toast.
- Any other failure is treated as a transient bootstrap failure and must render a retryable error state for protected-route entry instead of silently pretending the user is anonymous.
- `login` and `register` both end in `authenticated` state using the returned server payload.
- `logout` calls the backend endpoint, clears local auth state, and returns the app to anonymous routing.

### 2. Public auth routes

Add two public routes outside `AppShell`:

- `/login`
- `/registro`

Requirements:

- Forms are in Spanish.
- Minimum fields:
  - Login: `email`, `password`
  - Register: `email`, `password`, `confirmPassword`
- Registration must block submit when `password !== confirmPassword`.
- Server validation and auth failures must be surfaced with safe user-facing copy in Spanish.
- If an authenticated user visits `/login` or `/registro`, redirect them to the default authenticated landing page (`/`) or to a preserved intended destination when present.

### 3. Route guard

All existing app-shell routes remain protected.

Protected area:

- `/`
- `/mediciones`
- `/entrenamiento`
- `/nutricion`
- `/lista-compra`
- `/progreso`
- `/objetivos`
- `/ajustes`
- `/ajustes/integraciones`

Public area:

- `/login`
- `/registro`
- `/auth` (existing Withings callback route remains public)

Guard behavior:

- While auth bootstrap is unresolved, protected navigation renders a loading state, not the real shell.
- Anonymous access to protected routes redirects to `/login`.
- The redirect preserves the original destination so login can return the user there afterward.
- Unknown routes should keep the current not-found behavior within the correct public/protected context.

### 4. CSRF-aware API client

Enhance `frontend/src/api/client.ts` instead of scattering auth logic through feature modules.

Required behavior:

1. Every request made through the shared client sends cookies.
2. Credential mode must be:
   - `same-origin` when `baseUrl` is empty/relative or resolves to the current origin
   - `include` when `baseUrl` points to a different origin
3. For unsafe requests, the client must:
   - ensure an `XSRF-TOKEN` cookie exists before the request is sent
   - read the cookie value from `document.cookie`
   - send it as `X-XSRF-TOKEN`
4. If no `XSRF-TOKEN` cookie exists yet, the client must prime it with `GET /actuator/health` before the unsafe request.
5. Safe requests (`GET`, `HEAD`, `OPTIONS`) must not send `X-XSRF-TOKEN`.
6. `requestBlob` must also send credentials so authenticated binary reads keep working.

This slice may add a small internal helper layer inside the client, but it must preserve the centralized API boundary from FOR-81.

### 5. Current-user bootstrap

The app root must mount the auth provider before route resolution depends on auth state.

Requirements:

- Bootstrap uses `GET /api/v1/auth/me` with credentials.
- The request must not be treated as logout on non-401 failures.
- Protected-route entry must not flash the authenticated shell before bootstrap finishes.
- Public auth pages may render while bootstrap is loading, but they must redirect away if bootstrap resolves authenticated.

### 6. Topbar session display

Replace the static account area in `Topbar` with real session UI.

Requirements:

- Show the authenticated user's email.
- Keep the existing theme toggle and notifications affordance.
- Add a logout action reachable by keyboard and screen reader.
- Remove copy/comments that still describe FORMA as single-user/no-auth.

User-facing notes in Spanish:

- Login heading example: `Iniciar sesión`
- Register heading example: `Crear cuenta`
- Logout action example: `Cerrar sesión`
- Bootstrap/guard pending state example: `Comprobando tu sesión...`
- Generic auth submit failure example: `No se pudo iniciar la sesión. Inténtalo de nuevo.`

## Testing scope

Minimum automated coverage for this slice:

1. API client tests
   - credentials mode for same-origin requests
   - credentials mode for cross-origin requests
   - unsafe request primes `XSRF-TOKEN` via `GET /actuator/health` when absent
   - unsafe request sends `X-XSRF-TOKEN` when cookie exists
   - safe requests do not send `X-XSRF-TOKEN`
   - `requestBlob` sends credentials
2. Auth state tests
   - bootstrap to authenticated on `me=200`
   - bootstrap to anonymous on `me=401`
   - bootstrap retryable error on non-401 failure
   - `login`, `register`, and `logout` update state correctly
3. Route tests
   - anonymous user hitting a protected route is redirected to `/login`
   - authenticated user reaching `/login` or `/registro` is redirected away
   - post-login redirect returns the user to the intended protected route
4. Topbar/auth UI tests
   - authenticated topbar shows the real email instead of static `Diego`
   - logout action is rendered and callable
   - login/register forms show Spanish validation or backend error messages

## Non-goals

- No backend auth endpoint changes.
- No password reset, email verification, remember-me, or profile editing.
- No role/permission model.
- No changes to Withings OAuth callback behavior beyond coexistence with the new auth routes.
- No cross-user isolation proof across all domains; that belongs to FOR-145e.
- No redesign of the full application shell beyond the minimal auth-state UI needed here.

## Acceptance criteria

1. Anonymous users cannot reach any existing app-shell route without being redirected to `/login`.
2. Users can register, log in, and log out against the existing backend auth endpoints from the SPA.
3. The SPA correctly primes and sends CSRF data for unsafe requests and always sends session cookies.
4. App bootstrap restores an existing server session via `GET /api/v1/auth/me` without flashing protected content to anonymous users.
5. `Topbar` no longer shows static identity; it renders the authenticated email and a logout action.
6. New or updated frontend tests cover the auth state, route guard, and CSRF-aware client behavior.
7. No production backend code is changed as part of this slice.
