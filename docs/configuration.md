# Configuration and Secrets

The single reference for FORMA's environment variables and secret handling
(FOR-90). Aligned with [ADR-002](adr/ADR-002-authentication.md) and the
forbidden-shortcuts section of `AGENTS.md`.

## Principles

- **No real secrets in git.** Only example files with clearly fake values are
  committed. Local secret files (`.env`) are gitignored.
- **Environment-driven.** Configuration comes from environment variables, with
  safe local defaults for developer convenience.
- **Fail fast in production.** Under the `prod` profile, critical configuration
  is required — a missing value stops startup with a clear message instead of
  silently using an unsafe default.
- **Backend secrets never reach the frontend.** Only `VITE_*` variables are
  bundled into the frontend; they are public. Never put a secret in a `VITE_*`
  variable.

## Environment variables

Example values live in [`env.example`](../env.example) (root). Copy it to `.env`
to override locally; `.env` is gitignored.

| Variable | Scope | Default (local) | Secret? | Notes |
| --- | --- | --- | --- | --- |
| `POSTGRES_DB` | infra | `forma` | no | Database name (Compose). |
| `POSTGRES_USER` | infra | `forma` | no | Database user (Compose). |
| `POSTGRES_PASSWORD` | infra/backend | `forma_local_password` | **yes** | Local-only fake value; a real one is required in `prod`. |
| `POSTGRES_PORT` | infra | `5432` | no | Published Postgres port. |
| `BACKEND_PORT` | infra | `8080` | no | Published backend port. |
| `FRONTEND_PORT` | infra | `3000` | no | Published frontend port. |
| `SPRING_DATASOURCE_URL` | backend | derived (localhost) | no | JDBC URL; **required in `prod`**. |
| `SPRING_DATASOURCE_USERNAME` | backend | `forma` | no | **Required in `prod`**. |
| `SPRING_DATASOURCE_PASSWORD` | backend | `forma_local_password` | **yes** | **Required in `prod`**. |
| `SPRING_PROFILES_ACTIVE` | backend | (none → local defaults) | no | Set to `prod` for real deployments. |
| `VITE_API_BASE_URL` | **frontend (public)** | `http://localhost:8080` | no | Bundled into the SPA; never a secret. |
| `WITHINGS_CLIENT_ID` | backend | (empty) | no | Withings application id. Empty disables connecting, with a named error. |
| `WITHINGS_CLIENT_SECRET` | backend | (empty) | **yes** | Withings application secret. |
| `WITHINGS_REDIRECT_URI` | backend | `https://forma.diegobarrioh.dev/auth` | no | Must match the URI registered with Withings exactly. |
| `WITHINGS_TOKEN_ENC_KEY` | backend | (empty) | **yes** | Key for encrypting stored provider tokens. `openssl rand -base64 32`. |
| `FORMA_PLAN_AGENT_BASE_URL` | backend | (empty) | no | The external AI plan agent's endpoint (ADR-015 decision 10). Empty disables `PlanGenerationGateway#generate`, with a named `PlanGenerationException` at the point of use — nothing calls it yet (slice 6 wires the dispatcher). |
| `FORMA_PLAN_AGENT_API_KEY` | backend | (empty) | **yes** | The plan agent's API key, sent as an `Authorization: Bearer` header. Never logged (ADR-008). |
| `FORMA_PLAN_AGENT_TIMEOUT_SECONDS` | backend | `120` | no | HTTP request timeout for the plan agent call. The ADR's own proposed default — an explicit, unmeasured guess (writing a week of meals is a slower call than Withings' 10 s Getmeas lookup); override once the first real integration measures it. |
| `FORMA_BOOTSTRAP_LEGACY_USER_PASSWORD` | backend | (empty) | **yes** | Activates the pre-auth placeholder account (FOR-145). |
| `GOOGLE_CLIENT_ID` | backend | (empty) | no | Google OAuth client id. Empty disables `oauth2Login()`; `/api/oauth2/authorization/google` redirects to `/login?error=google` instead of 404/500ing. |
| `GOOGLE_CLIENT_SECRET` | backend | (empty) | **yes** | Google OAuth client secret. |
| `FORMA_FRONTEND_URL` | backend | (empty) | no | Where the backend redirects the browser after a Google login (or a failed one, with `?error=google`). Empty resolves to a same-origin **relative** redirect (`/app`, `/login?error=google`), which is correct in every supported flow — prod and compose both proxy `/api/` to the backend behind one nginx origin, and Vite's dev server proxies it the same way for plain `npm run dev`. Only needs a value to exercise Google login against the backend directly, bypassing every proxy. |

### Google OAuth client — authorized redirect URIs

Register **one redirect URI per origin the SPA is actually served from** in the
Google Cloud Console client (`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`
above) — `SecurityConfig#googleClientRegistration`'s `redirect-uri` template
(`{baseUrl}/api/login/oauth2/code/{registrationId}`) resolves `{baseUrl}` from
the inbound request's `X-Forwarded-Host`/`X-Forwarded-Proto`
(`server.forward-headers-strategy=framework`), which both
`frontend/vite.config.ts`'s dev proxy (`xfwd: true`, verified empirically —
see its commit) and `frontend/nginx.conf` set — the latter now *preserving* an
incoming `X-Forwarded-Proto`/`X-Forwarded-Host` from a public proxy in front of
it rather than overwriting it, a production bug fixed for the double-reverse-
proxy topology (see [ADR-014](adr/ADR-014-google-login.md) point 13):

| Environment | Authorized redirect URI |
| --- | --- |
| Production | `https://forma.diegobarrioh.dev/api/login/oauth2/code/google` — this is currently a domain [`PreproRibbon`](../frontend/src/layout/preproHost.ts) itself recognizes as preproduction (`diegobarrioh.dev`); there is no separate staging host to register in addition to it. |
| Docker Compose | `http://localhost:3000/api/login/oauth2/code/google` (the published frontend port) |
| Docker Compose, alternate local/preprod port | `http://localhost:3002/api/login/oauth2/code/google` — `compose.yaml`'s `FORMA_CORS_ALLOWED_ORIGINS` default already includes `http://localhost:3002` for this; set `FRONTEND_PORT=3002` to serve the frontend there (see `docs/plans/FOR-145d-frontend-auth-state.md`'s manual test steps). |
| `npm run dev` (no Compose) | `http://localhost:5173/api/login/oauth2/code/google` (Vite's dev server port) |

Hitting the backend directly on `http://localhost:8080` (bypassing every
proxy) is not a supported way to exercise Google login and has no registered
redirect URI by default — it only works if `FORMA_FRONTEND_URL` is also set to
an absolute origin.

### Getting a variable into the backend container

Listing a variable in `.env` is not enough. Compose reads `.env` to interpolate
`${...}` **into `compose.yaml` itself**; it does not forward that file to any
service. A variable only reaches the JVM if it also appears under the backend
service's `environment:` block. A value set in `.env` and missing from that block
is silently absent inside the container — which is exactly how a deployed backend
ended up building a Withings authorization URL with an empty `client_id`.

To check what a running container actually received:

```bash
docker compose exec backend printenv | grep WITHINGS
```

## Backend configuration loading

The backend reads configuration from the environment via Spring's relaxed
binding (`application.yml`). Two profiles:

- **default / local** — safe local defaults (matching Docker Compose), so
  `./gradlew bootRun` and `docker compose up` work with no setup.
- **`prod`** (`application-prod.yml`, `SPRING_PROFILES_ACTIVE=prod`) — datasource
  values are required from the environment with no fallback.

### Fail-fast

`CriticalConfigEnvironmentPostProcessor` runs before any bean is created. In the
`prod` profile it verifies `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`
and `SPRING_DATASOURCE_PASSWORD` are present; a missing one aborts startup with:

```text
Missing required configuration 'SPRING_DATASOURCE_PASSWORD' for the 'prod' profile...
```

The local and `test` profiles are not checked, so development stays frictionless.

## Frontend configuration loading

The frontend reads `VITE_API_BASE_URL` through the centralized API client
(`frontend/src/api/client.ts`), defaulting to `http://localhost:8080`. Vite
inlines `VITE_*` variables into the built bundle at build time, so they are
**public** — only non-secret values belong there.

## Logging and correlation IDs

Backend logging is structured around a per-request **correlation id** (FOR-91,
[ADR-008](adr/ADR-008-observability.md)).

- **`CorrelationIdFilter`** runs first on every request. It reads the
  `X-Correlation-Id` header or generates a UUID, sanitizes client-supplied
  values (safe characters only, capped length — prevents log forging), stores it
  in the SLF4J MDC and echoes it back on the response header.
- Every log line includes it via the console pattern
  (`logging.pattern.level`): `LEVEL [forma-backend,<correlationId>] logger : message`.
- A minimal request log is emitted per request: `METHOD /path -> status (N ms)`.
  It logs the path only — never headers, query strings, bodies or personal data.
- API error responses carry the same correlation id (`ApiError.correlationId`,
  see [API conventions](api-conventions.md)), so a client-visible error can be
  matched to server logs.

### Sensitive logging rules (ADR-008)

- Never log passwords, access tokens, refresh tokens or provider secrets.
- Never log full personal health payloads.
- Health endpoints must not expose sensitive configuration.
- Keep the request log to non-sensitive metadata (method, path, status,
  duration).

## Secret handling rules

- Never commit real secrets, tokens, provider API keys or personal credentials
  (`AGENTS.md`). Example files use fake values only.
- `.env`, `.env.local`, `.env.*.local` are gitignored (root and `frontend/`).
- Never log credentials, access tokens or refresh tokens
  ([ADR-002](adr/ADR-002-authentication.md)).
- Never expose provider tokens or backend secrets to the frontend.
- Rotate any value that is ever committed by accident and treat it as
  compromised.
