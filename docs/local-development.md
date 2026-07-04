# Local Development

How to work on FORMA from a clean checkout.

> **Current repository status:** FORMA is in the **Project Bootstrap** phase.
> The backend skeleton (FOR-80), frontend skeleton (FOR-81), local Docker
> Compose environment (FOR-82) and PostgreSQL + Flyway migration baseline
> (FOR-83) now exist. Dedicated lint/format and test-suite tooling are still
> delivered by later bootstrap stories.
>
> Commands for components that are not scaffolded yet are marked
> **`PLANNED — not available yet`** and point to the story that will add them.
> Do not expect a `PLANNED` command to run until that story lands. This document
> must be updated as each bootstrap story merges.

## Required tools

| Tool | Version | Needed for | Notes |
| --- | --- | --- | --- |
| Git | any recent | Everything | — |
| Java (JDK) | 21 | Backend (`backend/`) | Per README stack and ADR-001. Exact distribution pinned by FOR-80. |
| Node.js | 20 LTS (recommended) | Frontend (`frontend/`) | Exact version to be pinned by FOR-81. |
| npm / pnpm | bundled with Node | Frontend | Package manager confirmed by FOR-81. |
| Docker | recent | Local infrastructure | Docker Desktop or Engine. |
| Docker Compose | v2 (`docker compose`) | Local infrastructure | Provided by FOR-82. |
| PostgreSQL client (optional) | 16+ | Inspecting the DB | Server runs in Docker; `psql` is only for manual inspection. |

Verify your toolchain:

```bash
git --version
java -version
node --version
docker --version
docker compose version
```

## Repository layout (target)

The intended layout once bootstrap stories land:

```text
frontend/     React + TypeScript + Vite                          (FOR-81)
backend/      Java 21 + Spring Boot 3 + PostgreSQL + Flyway       (FOR-80, FOR-83)
compose.yaml  Local Docker Compose environment (repo root)        (FOR-82)
docs/         Global documentation and ADRs                       (present)
specs/        Story-specific specs (specs/FOR-XXX/)               (present)
.ai/          Shared AI agent context                             (present)
```

Currently present: `backend/`, `frontend/`, `compose.yaml`, `docs/`, `specs/`,
`.ai/`, `AGENTS.md`, `README.md`.

## Getting the code

```bash
git clone git@github.com:guilu/forma.git
cd forma
```

## Infrastructure startup (Docker Compose)

The local environment is defined by `compose.yaml` at the repository root
(FOR-82). It provides three services on the `forma` network:

| Service | Image / build | Host port | Purpose |
| --- | --- | --- | --- |
| `postgres` | `postgres:17-alpine` | 5432 | Primary database (ADR-003), named volume `forma-postgres-data` |
| `backend` | built from `backend/Dockerfile` | 8080 | Spring Boot API, waits for `postgres` to be healthy |
| `frontend` | built from `frontend/Dockerfile` | 3000 | Static SPA served by nginx |

Requires **Docker Engine** with **Compose v2** (`docker compose`). Every setting
has a safe local default baked into `compose.yaml`, so a fresh checkout starts
with no extra configuration:

```bash
docker compose up --build -d   # build images and start services in the background
docker compose ps              # list services and health status
docker compose logs -f         # follow logs (add a service name to filter)
docker compose stop            # stop services (keeps containers and volumes)
docker compose down            # remove containers/network (keeps named volumes)
```

Once started: the API health check is at <http://localhost:8080/actuator/health>
and the frontend at <http://localhost:3000>.

### Environment variables

Defaults live in `compose.yaml`; override them by copying `env.example` to
`.env` (gitignored) and editing the values:

```bash
cp env.example .env
```

| Variable | Default | Used by |
| --- | --- | --- |
| `POSTGRES_DB` | `forma` | postgres, backend datasource |
| `POSTGRES_USER` | `forma` | postgres, backend datasource |
| `POSTGRES_PASSWORD` | `forma_local_password` | postgres, backend datasource |
| `POSTGRES_PORT` | `5432` | published postgres port |
| `BACKEND_PORT` | `8080` | published backend port |
| `FRONTEND_PORT` | `3000` | published frontend port |
| `VITE_API_BASE_URL` | `http://localhost:8080` | frontend build (browser → backend) |

These are **local-only fake credentials**. Do not commit real secrets; see the
forbidden-shortcuts section of `AGENTS.md`. The backend datasource variables are
wired now but consumed once FOR-83 adds the database driver and migrations.

## Database and migrations

PostgreSQL is the primary store (ADR-003) and schema evolution is
migration-driven with **Flyway** (FOR-83) — no manual schema changes.

**Migrations run automatically on backend startup.** Flyway applies any pending
files from `backend/src/main/resources/db/migration` before the application
finishes booting, so starting the backend (natively or via Docker Compose)
migrates the database:

```bash
docker compose up --build -d      # postgres + backend; backend applies migrations on start
docker compose logs -f backend    # watch Flyway apply pending migrations
```

Connection settings come from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`
and `SPRING_DATASOURCE_PASSWORD` (env-driven, with local defaults in
`application.yml` and `compose.yaml`). A fresh database is migrated from scratch;
re-running is a no-op once all versions are applied.

### Adding a migration

Create a versioned SQL file in `backend/src/main/resources/db/migration` using
the Flyway naming convention:

```text
V<version>__<snake_case_description>.sql   e.g. V2__add_body_measurements.sql
```

- `V` prefix, an increasing integer version, then `__` (double underscore) and a
  short description.
- Never edit an already-applied migration — add a new versioned file instead.
- The current baseline is `V1__baseline.sql` (empty on purpose: it only
  establishes Flyway history; product tables arrive with their own stories).

Migrations are verified in tests against an in-memory H2 database (PostgreSQL
mode), so `cd backend && ./gradlew test` proves the baseline applies from
scratch without Docker.

## Backend startup

The backend is a Java 21 + Spring Boot 3 application built with Gradle (FOR-80,
ADR-001). To run it natively (outside Docker):

```bash
cd backend
./gradlew build     # compile and run tests
./gradlew bootRun   # start the app on http://localhost:8080
```

The skeleton exposes an Actuator health endpoint at
<http://localhost:8080/actuator/health> (FOR-80). Inside Docker Compose the
backend runs as the `backend` service instead.

## Frontend startup

The frontend is React + TypeScript + Vite (FOR-81, ADR-006). To run the dev
server natively (outside Docker):

```bash
cd frontend
npm install
npm run dev        # start the Vite dev server on http://localhost:5173
```

The API base URL is read from `VITE_API_BASE_URL` (default
`http://localhost:8080`); see `frontend/README.md`. Inside Docker Compose the
frontend is built and served by nginx as the `frontend` service on port 3000.

## Test commands

**PLANNED — not available yet (FOR-86 backend, FOR-87 frontend).**

FORMA uses a layered testing strategy (ADR-007). Expected shape:

```bash
# PLANNED (FOR-86) — backend tests
cd backend && ./gradlew test

# PLANNED (FOR-87) — frontend tests / type check
cd frontend && npm test
```

Test naming conventions are documented by FOR-86 and FOR-87.

## Lint and format commands

**PLANNED — not available yet (FOR-85).**

Formatting and linting baselines for backend and frontend are delivered by
FOR-85. Expected shape:

```bash
# PLANNED (FOR-85)
# backend:  formatting/lint check + apply tasks (tooling chosen by FOR-85)
# frontend: npm run lint   /   npm run format
```

## Continuous integration

CI runs on every pull request and on pushes to `main` (FOR-84), defined in
`.github/workflows/ci.yml`. It runs the same commands you run locally, as two
independent jobs — either failing fails the pipeline:

- **Backend** — `cd backend && ./gradlew build` (compile + tests).
- **Frontend** — `cd frontend && npm ci && npm run typecheck && npm test && npm run build`.

Gradle and npm dependencies are cached between runs. Lint/format checks join the
pipeline once FOR-85 configures them. Keep CI green — see the merge policy in
`docs/branching-strategy.md`.

## Local reset procedure

Use these to return to a clean local state.

Reset the working tree (discards uncommitted changes — **destructive**):

```bash
git status            # review what you are about to lose
git stash             # OR: keep changes aside instead of discarding
git checkout .        # discard tracked-file changes
git clean -fdx        # remove untracked/ignored files (very destructive)
```

Reset local infrastructure and data (FOR-82):

```bash
docker compose down -v          # remove containers AND named volumes (deletes local DB data)
docker compose up --build -d    # rebuild and start fresh
```

Named volume data (`forma-postgres-data`) persists across `docker compose stop`
/ `down`; only `down -v` deletes it. After a data reset, re-run migrations
(**PLANNED — FOR-83**) to rebuild the schema from scratch.

## Troubleshooting

| Symptom | Likely cause | Action |
| --- | --- | --- |
| A `PLANNED` command is not found | The bootstrap story that adds it has not merged yet | Check the referenced `FOR-XXX` story; the component does not exist yet |
| `java -version` shows the wrong major version | Multiple JDKs installed | Point `JAVA_HOME` at JDK 21 |
| `docker compose` reports "command not found" | Compose v1 or Docker not installed | Install Docker with Compose v2 (`docker compose`, not `docker-compose`) |
| Port already in use on startup | Another local process/container holds the port | Stop the other process or change the configured port |
| Database connection refused | Postgres container not running | `docker compose ps`, then `docker compose up -d` |
| Local DB in a bad state | Stale volume data | Run the local reset procedure above |

## Known limitations

- The backend skeleton (FOR-80), frontend skeleton (FOR-81) and Docker Compose
  environment (FOR-82) exist. Database migrations (FOR-83), lint/format (FOR-85)
  and dedicated test suites (FOR-86/FOR-87) are still **planned** and marked as
  such above.
- The backend now connects to PostgreSQL and applies Flyway migrations on
  startup (FOR-83). The `V1__baseline.sql` migration is intentionally empty —
  product tables arrive with their own stories.
- Automated migration verification runs against in-memory H2 (PostgreSQL mode)
  so `./gradlew test` needs no Docker. Running migrations against real
  PostgreSQL happens when the backend starts via Docker Compose.
- Exact lint/format and dedicated test-suite tasks are decided by their owning
  stories (FOR-85/FOR-86/FOR-87) and are marked as such above.
- This is a local development guide only. Production operations, cloud deployment
  and a full contributor handbook are out of scope (see FOR-89 spec).

## Keeping this document current

Update this file whenever a bootstrap story lands:

1. Replace the relevant `PLANNED` block with the verified, copy-pasteable command.
2. Remove the `PLANNED — not available yet` marker for that component.
3. Update the "Current repository status" note and "Known limitations" as the
   repository stops being documentation-only.

## References

- `AGENTS.md` — agent entry point and required reading order
- `README.md` — product idea and proposed stack
- `docs/architecture-overview.md`, `docs/adr/ADR-001-architecture.md` — architecture
- `docs/adr/ADR-003-persistence.md` — persistence and migrations
- `docs/adr/ADR-006-frontend.md` — frontend
- `docs/adr/ADR-007-testing.md` — testing strategy
- `docs/coding-standards.md`, `docs/definition-of-done.md`
- Bootstrap specs: `specs/FOR-80`…`specs/FOR-88`
