# ADR-014: Login with Google

## Status

Accepted. Extends [ADR-012](ADR-012-authentication-and-multi-user-isolation.md) — the session-
cookie mechanism, `users` schema and `CurrentUserProvider`/`FormaUserPrincipal` design stay
exactly as ADR-012 describes them. This ADR only adds a second way to *establish* that session.

## Context

ADR-012 gave FORMA email/password authentication. Adding "Continuar con Google" was requested
next, following the shape already proven in the `akademia` project (a Spring Boot app using
`spring-boot-starter-oauth2-client`'s server-side authorization-code flow), but adapted: akademia
is JWT/stateless and issues its tokens to the SPA through a one-time exchange code appended to the
callback redirect (`?code=...`); FORMA is session-cookie based (ADR-012 Decision 1), so there is no
token to hand the browser at all — the existing session machinery does the whole job.

## Decision

1. **Flow**: Spring Security's standard server-side OAuth2/OIDC authorization-code flow
   (`spring-boot-starter-oauth2-client`, `HttpSecurity#oauth2Login`), not a client-side/implicit
   flow and not a custom hand-rolled redirect dance. Mounted under `/api` —
   `/api/oauth2/authorization/google` (start) and `/api/login/oauth2/code/google` (callback) — so
   the single nginx `location /api/` proxy rule already covers both and the SPA's client-side
   router never intercepts them (the akademia gotcha this avoids: endpoints outside `/api` broke in
   prod because the SPA's catch-all served `index.html` instead of reaching the backend).
2. **No exchange code, ever** (deliberately diverges from akademia). akademia's JWT flow has to
   hand the SPA a token somehow, and a query-string exchange code was its fix for an earlier
   mistake (a raw JWT in the redirect URL, visible in browser history/referrer/proxy logs).
   FORMA's `GoogleOAuth2SuccessHandler` instead reuses the exact session-establishing sequence
   `AuthController#login` already uses (extracted into `SessionAuthenticator`, delivery/security):
   rotate the session id, build a `SecurityContext` whose `Authentication` principal is a
   `FormaUserPrincipal` — not the raw `OidcUser`/`OAuth2AuthenticationToken` Spring Security would
   otherwise put there, which `SecurityContextCurrentUserProvider` does not understand — and save it
   via the existing `SecurityContextRepository`. Nothing token-like ever crosses the browser or a
   URL.
3. **Account resolution** (`UserService#loginWithGoogle`, application layer — framework-free,
   ADR-001): reject unless Google's own `email_verified` claim is `true` and non-blank; look up by
   the Google `sub` claim first; failing that, look up by normalized email and **link** the `sub` to
   that existing account (e.g. one created by self-registration); failing that, create a new active
   `USER`-role account with no password. Reject an inactive account either way. This is the second
   akademia gotcha deliberately not repeated: akademia links purely by email, with no
   `email_verified` check and no stored provider subject at all — an unverified or reused address
   could silently take over an existing account. Storing the subject (never reused, never
   attacker-influenced the way an email string can be) as the primary join key, gated on
   `email_verified`, closes that gap.
   - **Re-link takeover guard** (fresh-review fix): the email-match branch above rejects, rather
     than overwrites, when the account it found is already linked to a *different* Google subject —
     the first implementation silently reassigned it, which would let a second Google identity take
     over an existing account. `JdbcUserRepository#linkGoogleSubject` also guards the same case at
     the SQL level (`UPDATE ... WHERE id = ? AND google_subject IS NULL`, returning whether it
     actually changed a row), so the invariant holds even if a future caller reaches the repository
     without going through `UserService`'s check first.
4. **Schema** (migration V62): `users.password_hash` becomes nullable (a Google-only account has
   none — never a fabricated or placeholder hash) and a nullable, unique `users.google_subject`
   column stores the Google `sub`. `DelegatingPasswordEncoder`'s own null-encoded-password guard
   (routed through `SecurityConfig`'s `defaultPasswordEncoderForMatches`) already returns `false`
   rather than throwing for a null `password_hash`, so a password-login attempt against a
   Google-only account is an ordinary 401, not a 500 — verified by a regression test
   (`AuthenticationFlowIntegrationTest`), not just asserted.
5. **Concurrency**: two simultaneous first-time Google logins for the same identity can both pass
   the initial lookup and then collide on the unique constraint (`google_subject` or `email`) at
   insert/link time. `UserService#loginWithGoogle` catches that `DataIntegrityViolationException`
   and retries once by re-resolving — the loser's collision means the winner's row now exists to
   find. Same pattern akademia uses for its equivalent race.
6. **Configuration** (`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`, empty by default): Spring Boot's
   own `spring.security.oauth2.client.registration.*` property binding fails application startup
   on an empty `client-id` — unacceptable, since every environment that has not configured Google
   yet (including CI) must still boot and pass. `SecurityConfig` instead reads
   `forma.oauth2.google.*` via plain `@Value` and builds the `ClientRegistration`
   (`CommonOAuth2Provider.GOOGLE`'s defaults, `redirect-uri={baseUrl}/api/login/oauth2/code/{registrationId}`)
   by hand, registering `oauth2Login()` only when both values are non-blank. `SecurityConfig` also
   logs a warning (never the values) when exactly one of the two is set — a copy-paste
   misconfiguration that would otherwise leave `oauth2Login()` silently disabled with no signal
   anywhere. When not configured, `GoogleOAuth2FallbackController` answers
   `/api/oauth2/authorization/google` with a redirect to the login page (point 8) instead of a bare
   404 — the two paths never conflict, because Spring Security's authorization-redirect filter
   (present only when configured) always intercepts the request before it reaches that controller.
7. **`{baseUrl}` behind a proxy**: `server.forward-headers-strategy=framework` makes the
   redirect-uri template resolve `{baseUrl}` from `X-Forwarded-Proto`/`X-Forwarded-Host` instead of
   the backend's own plain-http, internal view of itself — `frontend/nginx.conf` sends both in prod
   and Compose, and `frontend/vite.config.ts`'s dev proxy sends both for plain `npm run dev` (point
   12). Register **one Google Cloud Console redirect URI per origin the SPA is actually served
   from** (docs/configuration.md has the current list: prod, Compose's `:3000`, and Vite's `:5173`)
   — the backend builds `redirect_uri` from whichever origin the request actually arrived through,
   so each one needs to be registered, not just the production one.
8. **Post-login/error destination and `forma.frontend-url`** (revised by the fresh-review fix
   below): redirects to the SPA's existing default authenticated destination (`/app`,
   `authDestination.ts`) on success, or `/login?error=google` on any rejection. The pre-login "from"
   location (preserved across a normal password login via router state) is **not** preserved across
   the Google redirect round trip — documented as a known limitation rather than solved with a
   stashed-server-side-state mechanism this MVP does not otherwise need.

   `forma.frontend-url`/`FORMA_FRONTEND_URL` defaults to **empty**, which
   `FrontendRedirectResolver` (delivery/security) then resolves as a same-origin **relative**
   redirect target rather than prefixing an absolute origin. An earlier version of this decision
   defaulted it to `http://localhost:5173` — caught in fresh review as finding #3: a real deployment
   that forgot to set it would send every user's browser to `localhost` after login, a silent
   footgun with no fail-fast signal. Checked before choosing the fix: `frontend/nginx.conf` proxies
   `/api/` to this backend behind one origin in **both** production and Docker Compose
   (`compose.yaml`'s frontend service). An absolute `forma.frontend-url` is needed only for the one
   flow that genuinely differs: hitting this backend directly, bypassing every proxy (e.g. `./gradlew
   bootRun` with no frontend running) — not a supported way to exercise Google login, so it stays
   opt-in rather than becoming a new required-in-prod variable (which would have reintroduced the
   same class of footgun this fix removes: a required var is only as safe as remembering to set it).

   Plain `npm run dev` needed a *second* fix before this held for it too (point 12) — its Vite dev
   proxy was not forwarding `X-Forwarded-Host` at all, so the relative-redirect claim above was not
   actually true for that flow until that fix landed.
9. **A business-rule rejection inside the success handler must not surface as a 500** (fresh-review
   fix): `AbstractAuthenticationProcessingFilter` only catches `AuthenticationException` around the
   call into `GoogleOAuth2SuccessHandler` — an `UnauthorizedException` from
   `UserService#loginWithGoogle` (unverified email, inactive account, the re-link guard in point 3)
   would otherwise propagate out uncaught. The handler now catches it, calls
   `SessionAuthenticator#clearSession` to tear down the session Spring Security's filter already
   half-built *before* the handler ran (it persists the raw `OidcUser`-principal `Authentication`
   into the session ahead of calling `successfulAuthentication`, regardless of what the handler goes
   on to do), and redirects to the same `/login?error=google` page the `oauth2Login` failure handler
   uses — never putting anything from the exception itself into that redirect.
10. **A blank/missing email is rejected even when `email_verified: true`** (fresh-review fix,
    SUGGESTION): that claim only promises the address is verified, not that one was sent at all.
    `UserService#loginWithGoogle` checks this before the `email_verified` check's neighbour ever
    reaches a repository call.
11. **End-to-end coverage of the success path** (fresh-review fix):
    `GoogleOAuth2LoginEndToEndIntegrationTest` drives the real `GoogleOAuth2SuccessHandler` bean —
    real `UserService`/`JdbcUserRepository`/`SessionAuthenticator`/`SecurityContextRepository` — with
    a real `OAuth2AuthenticationToken`/`DefaultOidcUser`, and checks the database row, the rotated
    session id, the persisted `SecurityContext`'s `FormaUserPrincipal`, and the redirect target. It
    stops short of a literal `GET /api/v1/auth/me` HTTP round trip: doing that would need either a
    hand-built session wired into a real embedded Tomcat's own session store (not possible — they are
    unrelated stores) or a local stub of Google's authorization/token/userinfo endpoints with signed
    ID tokens and a JWKS, which is not simple to stand up correctly. Instead it asks the real
    `SecurityContextRepository` bean to reload the context from the same session — the exact
    operation a follow-up request's `SecurityContextHolderFilter` performs — which is the closest
    practical proof available that `/auth/me` would in fact see that principal.
12. **Plain `npm run dev` was still broken after point 8's fix** (fresh-review follow-up): Vite's
    dev proxy (`vite.config.ts`) had `changeOrigin: true` but nothing forwarding `X-Forwarded-*` at
    all, so the backend saw a plain `Host: localhost:8080` with no `X-Forwarded-Host` — `{baseUrl}`
    (point 7) resolved to the backend's own port, Google was asked for
    `http://localhost:8080/api/login/oauth2/code/google`, and the post-login relative redirect
    (point 8) landed the browser on the *backend* (which serves no SPA) instead of Vite's `:5173`.
    Fixed with `xfwd: true` on the proxy entry — verified empirically, not by reading Vite's docs:
    a throwaway Node HTTP echo server was pointed at by the proxy target, and a `curl` through Vite
    showed `x-forwarded-host: localhost:5173`, `x-forwarded-proto: http` and
    `x-forwarded-port: 5173` on the request the echo server actually received; no extra `configure`
    hook was needed. `GoogleOAuth2ConfiguredIntegrationTest`'s new
    `authorizationPathBuildsTheRedirectUriFromXForwardedHost` regression-tests the backend half:
    sends `X-Forwarded-Host: localhost:5173` and asserts the resulting `redirect_uri` query param
    sent to Google is exactly `http://localhost:5173/api/login/oauth2/code/google`.

    **Trust boundary this relies on**: with `forward-headers-strategy=framework`, the backend
    trusts `X-Forwarded-*` from *whoever reaches it* — it has no way to tell a header set by
    `nginx`/Vite's proxy apart from one set by an attacker with direct access to the backend port.
    The impact here is limited: Google validates `redirect_uri` against the exact list registered
    for the client (point 7), so a forged header can at most redirect the *initial* authorization
    request to an unregistered URI, which Google itself then refuses; and every redirect this app
    issues off the back of it is a same-origin *relative* path (point 8), never an attacker-supplied
    absolute one. The mitigation is deployment hygiene, not application code: **the backend port
    must not be publicly reachable in production** — only the proxy that legitimately sets these
    headers should ever be able to reach it. This ADR does not change the deployment to enforce
    that; it is a precondition the decision above assumes.

## Consequences

- A FORMA account can now exist with `password_hash IS NULL` (Google-only). Every future query or
  feature that assumes a password always exists must account for this — the immediate instance is
  password login itself (point 4 above); there are no others yet, since nothing else in FORMA reads
  `password_hash`.
- `users.google_subject` is a second unique identity axis alongside `email`; a future account
  merge/unlink feature would need to consider both.
- The frontend gets one new external link (`Continuar con Google`, `GET
  {apiBase}/api/oauth2/authorization/google`) on both `LoginPage` and `RegisterPage` — a real
  top-level navigation, not a fetch, so it is unaffected by CSRF/CORS (both only apply to
  same-origin `fetch`/XHR calls the API client makes).

## Rules

- Never put a token, session id, or any other credential-like value in a URL, query string, or
  redirect target (ADR-012's existing rule, reaffirmed for this flow specifically since it is the
  one place a naive implementation — akademia's earlier mistake — most easily violates it).
- Every Google login must check `email_verified` before creating or linking any account.
- The application layer resolves the account (`UserService`); the delivery layer only adapts
  Spring Security types to that call and back (ADR-001 boundary, same as every other use case).
