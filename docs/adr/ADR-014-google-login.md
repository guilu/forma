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
   ADR-001): reject unless Google's own `email_verified` claim is `true`; look up by the Google
   `sub` claim first; failing that, look up by normalized email and **link** the `sub` to that
   existing account (e.g. one created by self-registration); failing that, create a new active
   `USER`-role account with no password. Reject an inactive account either way. This is the second
   akademia gotcha deliberately not repeated: akademia links purely by email, with no
   `email_verified` check and no stored provider subject at all — an unverified or reused address
   could silently take over an existing account. Storing the subject (never reused, never
   attacker-influenced the way an email string can be) as the primary join key, gated on
   `email_verified`, closes that gap.
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
   by hand, registering `oauth2Login()` only when both values are non-blank. When not configured,
   `GoogleOAuth2FallbackController` answers `/api/oauth2/authorization/google` with a redirect to
   `{frontendUrl}/login?error=google` instead of a bare 404 — the two paths never conflict, because
   Spring Security's authorization-redirect filter (present only when configured) always intercepts
   the request before it reaches that controller.
7. **`{baseUrl}` behind nginx**: `server.forward-headers-strategy=framework` plus
   `frontend/nginx.conf` forwarding `X-Forwarded-Proto`/`X-Forwarded-Host` is what lets the
   redirect-uri template resolve to the public `https://forma.diegobarrioh.dev` host rather than
   the backend container's own plain-http, internal view of itself.
8. **Post-login destination**: redirects to the SPA's existing default authenticated destination
   (`/app`, `authDestination.ts`). The pre-login "from" location (preserved across a normal
   password login via router state) is **not** preserved across the Google redirect round trip —
   documented as a known limitation rather than solved with a stashed-server-side-state mechanism
   this MVP does not otherwise need.

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
