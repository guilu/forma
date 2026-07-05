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
