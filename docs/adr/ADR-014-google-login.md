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
7. **`{baseUrl}` behind nginx**: `server.forward-headers-strategy=framework` plus
   `frontend/nginx.conf` forwarding `X-Forwarded-Proto`/`X-Forwarded-Host` is what lets the
   redirect-uri template resolve to the public `https://forma.diegobarrioh.dev` host rather than
   the backend container's own plain-http, internal view of itself.
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
   (`compose.yaml`'s frontend service), and `vite.config.ts`'s `server.proxy['/api']` does the same
   for plain `npm run dev` — in all three supported flows the browser only ever talks to the
   frontend's own origin, so a relative `Location` header is correct with zero configuration. An
   absolute `forma.frontend-url` is needed only for the one flow that genuinely differs: hitting
   this backend directly, bypassing every proxy (e.g. `./gradlew bootRun` with no frontend running)
   — not a supported way to exercise Google login, so it stays opt-in rather than becoming a new
   required-in-prod variable (which would have reintroduced the same class of footgun this fix
   removes: a required var is only as safe as remembering to set it).
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
