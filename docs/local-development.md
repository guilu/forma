# Local Development

How to work on FORMA from a clean checkout.

> **Current repository status (FOR-89):** FORMA is in the **Project Bootstrap**
> phase. At the time of writing, the repository contains **documentation and
> story specs only**. The backend, frontend, Docker Compose environment,
> database migrations and test suites **do not exist yet** — they are delivered
> by the bootstrap stories listed below.
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
frontend/   React + TypeScript + Vite + TailwindCSS + Recharts   (FOR-81)
backend/    Java 21 + Spring Boot 3 + PostgreSQL + Flyway         (FOR-80, FOR-83)
infra/      Docker Compose first                                  (FOR-82)
docs/       Global documentation and ADRs                         (present)
specs/      Story-specific specs (specs/FOR-XXX/)                 (present)
.ai/        Shared AI agent context                               (present)
```

Currently present: `docs/`, `specs/`, `.ai/`, `AGENTS.md`, `README.md`.
Not present yet: `backend/`, `frontend/`, `infra/`.

## Getting the code

```bash
git clone git@github.com:guilu/forma.git
cd forma
```

## Infrastructure startup (Docker Compose)

**PLANNED — not available yet (FOR-82).**

Once FOR-82 lands, local services (PostgreSQL, and optionally backend/frontend)
will be started with Docker Compose. Expected shape:

```bash
# PLANNED (FOR-82) — will not work until the compose file exists
docker compose up -d        # start services in the background
docker compose ps           # list running services
docker compose logs -f      # follow logs
docker compose down         # stop services (keeps named volumes)
```

Environment variable examples and service names will be documented by FOR-82.
Do not commit real secrets; see `docs/adr/ADR-008-observability.md` and the
forbidden-shortcuts section of `AGENTS.md`.

## Database and migrations

**PLANNED — not available yet (FOR-83).**

PostgreSQL is the primary store (ADR-003). Schema evolution is migration-driven;
no manual schema changes. Migration tooling and the baseline migration are
delivered by FOR-83. Expected shape:

```bash
# PLANNED (FOR-83) — will not work until migration tooling is configured
# Run migrations against the local database
# (exact command — e.g. a Flyway/Gradle task — pinned by FOR-83)
```

Migration naming conventions will be documented by FOR-83.

## Backend startup

**PLANNED — not available yet (FOR-80).**

The backend is a Java 21 + Spring Boot 3 application (README, ADR-001). The
build tool and exact tasks are chosen by FOR-80. Expected shape:

```bash
# PLANNED (FOR-80) — will not work until the backend skeleton exists
cd backend
# build:   ./gradlew build   (or the Maven equivalent, decided by FOR-80)
# run:     ./gradlew bootRun
```

A health placeholder endpoint may be exposed by the skeleton (FOR-80).

## Frontend startup

**PLANNED — not available yet (FOR-81).**

The frontend is React + TypeScript + Vite (README, ADR-006). Expected shape:

```bash
# PLANNED (FOR-81) — will not work until the frontend skeleton exists
cd frontend
npm install
npm run dev        # start the Vite dev server
```

The dev server port and API client base URL will be documented by FOR-81.

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

## Local reset procedure

Use these to return to a clean local state.

Reset the working tree (discards uncommitted changes — **destructive**):

```bash
git status            # review what you are about to lose
git stash             # OR: keep changes aside instead of discarding
git checkout .        # discard tracked-file changes
git clean -fdx        # remove untracked/ignored files (very destructive)
```

Reset local infrastructure and data — **PLANNED (FOR-82)**:

```bash
# PLANNED (FOR-82) — removes containers AND named volumes (deletes local DB data)
docker compose down -v
docker compose up -d
```

After a data reset, re-run migrations (**PLANNED — FOR-83**) to rebuild the
schema from scratch.

## Troubleshooting

| Symptom | Likely cause | Action |
| --- | --- | --- |
| A `PLANNED` command is not found | The bootstrap story that adds it has not merged yet | Check the referenced `FOR-XXX` story; the component does not exist yet |
| `java -version` shows the wrong major version | Multiple JDKs installed | Point `JAVA_HOME` at JDK 21 |
| `docker compose` reports "command not found" | Compose v1 or Docker not installed | Install Docker with Compose v2 (`docker compose`, not `docker-compose`) |
| Port already in use on startup | Another local process/container holds the port | Stop the other process or change the configured port |
| Database connection refused | Postgres container not running | `docker compose ps`, then `docker compose up -d` (once FOR-82 lands) |
| Local DB in a bad state | Stale volume data | Run the local reset procedure above |

## Known limitations

- The repository currently contains **documentation and specs only**. No runnable
  application, infrastructure, migrations or tests exist yet.
- All startup, migration, test and lint commands are **planned** and tied to the
  bootstrap stories FOR-80, FOR-81, FOR-82, FOR-83, FOR-85, FOR-86 and FOR-87.
  They cannot be verified from this document alone until those stories land.
- Exact versions (Node.js, package manager) and exact build tasks (Gradle vs
  Maven, migration command) are decided by their owning stories and are marked as
  such above.
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
