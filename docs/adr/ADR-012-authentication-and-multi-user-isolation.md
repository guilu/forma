# ADR-012: Authentication & multi-user isolation model

## Status

Accepted. Supersedes ADR-002's single-user MVP posture: authentication is now REAL, not
aspirational. ADR-002's rules (server-side ownership checks, reject cross-user reads/writes, no
UI-only security, no credential/token logging) remain in force and are implemented here. Realizes
ADR-011's binding assumption that FOR-145 delivers `users.id UUID PRIMARY KEY` and backfills the
`PLACEHOLDER_USER_ID` sentinel used by every v2 table. Consistent with ADR-003 (no JPA;
hand-written JDBC; Flyway; H2 `MODE=PostgreSQL`; one `ADD COLUMN` per `ALTER`; additive
migrations).

## Context

Today FORMA has zero auth stack: no `spring-security`/`jjwt`/`bcrypt` dependency; only
`CorsConfig`/`CryptoConfig`/`CriticalConfigEnvironmentPostProcessor` exist under `config/`. Eleven
application services each duplicate `public static final String OWNER_ID = "default-user"` and
resolve it internally — controllers never pass a principal. Five services
(`BodyMeasurementService`, `TrainingSessionStatusService`, `ShoppingProductService`,
`ShoppingListService`, `InsightHistoryService`) have zero owner-scoping at any layer — a live
multi-tenant data-isolation gap. The frontend has zero auth state (no login route, the API client
never sends credentials or a CSRF token). ADR-011's v2 tables (`plan`, `training_plan`, ...)
already use `user_id UUID` with a placeholder constant
(`00000000-0000-0000-0000-000000000000`) awaiting this story's `users` table and FK.

## Decision

1. **Mechanism**: server-side HTTP session via Spring Security, with an httpOnly, Secure,
   SameSite=Lax `JSESSIONID` cookie, and cookie-based CSRF
   (`CookieCsrfTokenRepository.withHttpOnlyFalse()` + `CsrfTokenRequestAttributeHandler`).
   Rejected alternatives: stateless JWT (client-side token storage reintroduces the XSS/CSRF
   problem this design avoids, and revocation needs a blocklist — solving a horizontal-scaling
   problem this self-hosted single-instance deployment does not have) and an opaque
   token-in-a-database scheme (reinvents the session mechanism Spring Security already provides,
   plus a per-request table read).
2. **Hashing**: Argon2id via Spring Security's `Argon2PasswordEncoder`, wrapped in a
   `DelegatingPasswordEncoder` (`{argon2}` id prefix) for algorithm agility. Parameters:
   `saltLength=16`, `hashLength=32`, `parallelism=1`, `memory=19456` KiB (~19 MiB),
   `iterations=2` — the OWASP-recommended Argon2id minimum (`m=19 MiB, t=2, p=1`). Explicit
   prohibition: **never** use `AesGcmTokenCipher` (the codebase's existing reversible symmetric
   cipher for OAuth provider tokens) to store passwords — reversible encryption is a
   looks-tempting-but-wrong primitive for credentials. BCrypt is registered as an acceptable
   fallback encoder (constrained self-hosted hardware where Argon2's memory cost is a problem);
   the `DelegatingPasswordEncoder` makes switching a one-line, backward-compatible change.
3. **`users` schema**: `id UUID PRIMARY KEY`, `email VARCHAR(320) NOT NULL` with a unique index,
   `password_hash VARCHAR(255) NOT NULL`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT
   CURRENT_TIMESTAMP`, `last_login_at TIMESTAMP WITH TIME ZONE NULL`, `is_active BOOLEAN NOT NULL
   DEFAULT TRUE`.
4. **Principal resolution**: a small application-layer port,
   `CurrentUserProvider#currentUserId(): UUID`, backed by `SecurityContextHolder` in a delivery
   adapter. It replaces each service's duplicated `OWNER_ID` constant via constructor injection —
   controllers stay thin (no principal argument). Introduced in this slice; wiring it into the 11
   existing owner-scoped services is deliberately deferred to the next slice (145b) to keep this
   change reviewable.
5. **Phased `owner_id` → `user_id` migration** (expand/migrate/contract), covered end to end by
   this ADR but executed across later slices: this slice (145a) only adds `users` — it makes no
   change to any existing `owner_id` column. A placeholder-UUID row seeds the legacy
   `'default-user'` data owner so later slices can backfill against it.
6. **Registration**: public self-registration (email + password), consistent with the multi-user
   product direction — no invite/admin gate.

### Spring Security filter chain

- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `CsrfTokenRequestAttributeHandler`
  (Spring Security 6 SPA pattern — the browser reads the `XSRF-TOKEN` cookie and echoes it as the
  `X-XSRF-TOKEN` header on non-GET requests). No CSRF exemptions: `register`/`login` are also
  protected: an authenticated priming `GET` (e.g. `/api/v1/auth/me`) issues the cookie before the
  SPA's first `POST`.
- **Session**: `SessionCreationPolicy.IF_REQUIRED`; session-fixation protection rotates the
  session id on login (`changeSessionId`).
- **Authorization**: `permitAll` on `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, and
  `GET /actuator/health`; `authenticated()` on everything else under `/api/v1/**` (including
  `GET /api/v1/auth/me` and `POST /api/v1/auth/logout`). The backend serves only `/api` — the SPA's
  static assets are served by nginx, not Spring — so no public static-asset rule is needed.
- **CORS**: origins are read from the existing `forma.cors.allowed-origins` property;
  `allowCredentials` flips to `true`; origins stay an explicit whitelist — **never** a wildcard
  (mandatory once credentials are enabled). `CorsConfig` now exposes a single
  `CorsConfigurationSource` bean consumed by `http.cors(...)`, replacing the previous
  `WebMvcConfigurer`-based mapping so there is one CORS source of truth.
- **Entry points**: a custom `AuthenticationEntryPoint` returns a 401 `ApiError` JSON body (never
  a login-page redirect); a custom `AccessDeniedHandler` returns a 403 `ApiError` JSON body. Both
  reuse the existing `ApiError`/`ApiErrorCode` shape (FOR-88).
- **Logout**: `logoutUrl("/api/v1/auth/logout")`, `invalidateHttpSession(true)`,
  `clearAuthentication(true)`, `deleteCookies("JSESSIONID")`, and a success handler that returns
  204 — server-side session invalidation, not a client-side cookie drop.
- **Cookie flags**: `server.servlet.session.cookie.http-only=true`,
  `server.servlet.session.cookie.secure=true`, `server.servlet.session.cookie.same-site=lax`.
- **Authentication provider**: `DaoAuthenticationProvider` backed by a custom
  `UserDetailsService` (loads by email via `JdbcUserRepository`) and the Argon2
  `PasswordEncoder`. An `AuthenticationManager` bean is exposed for the JSON login endpoint.

## Consequences

- Every `/api/v1/**` endpoint becomes authenticated. All existing `@SpringBootTest`/`@WebMvcTest`
  suites must authenticate their requests going forward — a large, expected blast radius tracked
  and migrated in the immediately following slice (this slice only records which pre-existing test
  classes now fail; it does not fix them).
- `ProgressPhotoService`'s current app-level 403 cross-owner check is expected to become a real
  per-user query filter (cross-user access → 404, no existence leak) once `CurrentUserProvider` is
  wired in (145b/c).
- Sets `users.id UUID PRIMARY KEY`; ADR-011's v2 tables can add their deferred
  `user_id → users(id)` foreign key in a later slice.

## Rules

- Reject unauthenticated requests to protected resources (401) and cross-user reads/writes (404,
  never leaking existence via 403).
- Never log credentials, session ids, CSRF tokens, or return `password_hash` in any response body.
- Hash passwords only with Argon2id via the `DelegatingPasswordEncoder`; never `AesGcmTokenCipher`.
- CORS: explicit origins only once credentials are enabled; never a wildcard.
- Migrations stay additive: one `ADD COLUMN` per `ALTER`; backfill before adding `NOT NULL`/FK
  constraints (applies to later slices; this slice only creates `users`).

## Open points

- The phased `owner_id` (VARCHAR) → `user_id` (UUID) conversion for the 11 already-scoped tables,
  and the net-new `user_id` columns for the 5 zero-scoping tables, are designed in full but
  executed in the following slices (145b/145c) — not in this slice.
- Frontend auth state (login/register pages, route guard, CSRF-aware API client) is 145d.
- Cross-user isolation proof across all 16 domains is 145e.
