# AGENTS.md

This file is the entry point for AI coding agents working on FORMA.

## Project

FORMA is a personal health and fitness planning application. It combines body composition, training, nutrition, shopping, insights and external integrations into one explainable MVP.

Repository: `guilu/forma`
Jira project: `FOR`

## Current repository status

The bootstrap phase is over. Backend, frontend, database, Docker Compose environment and CI all exist and are in daily use; the application is being built feature by feature on top of them.

**This section describes what exists, not what is planned. Repository state has priority over roadmap/spec intent.** Specs describe the target for a story; the repository describes reality. If they differ, document the gap and do not invent missing code. Inspect the repository before assuming any component exists — including anything this file claims.

| Component | Where it lives |
|---|---|
| Backend | `backend/` — Gradle, hexagonal packages under `dev.diegobarrioh.forma` |
| Frontend | `frontend/` — Vite + React, CSS Modules |
| Migrations | `backend/src/main/resources/db/migration/` — Flyway, `V1..V60` and counting |
| Local environment | `compose.yaml` — Postgres + backend + frontend |
| CI | `.github/workflows/ci.yml` |
| Story specs | `specs/FOR-XXX/` |

## Project stack

Versions below are the ones the build files actually pin. Check the file named in each row before relying on a number here.

| Layer | Version | Source of truth |
|---|---|---|
| Java | 21 | `backend/build.gradle` (`languageVersion`) |
| Spring Boot | 3.3.5 | `backend/build.gradle` |
| Gradle | 8.14.1 (wrapper) | `backend/gradle/wrapper/gradle-wrapper.properties` |
| Node.js | 22 in CI | `.github/workflows/ci.yml` |
| React | 19 | `frontend/package.json` |
| TypeScript | 5.7 | `frontend/package.json` |
| Vite | 6 | `frontend/package.json` |
| Vitest | 3 | `frontend/package.json` |
| Playwright | 1.62 | `frontend/package.json` |
| PostgreSQL | 17 | `compose.yaml` |

Do not store technical configuration decisions in `.ai/`. The `.ai/` directory is shared context for agents, not the source of truth for stack versions or executable commands.

## Verification guidance

Run the checks that match what you touched. These are the same commands CI runs — see `.github/workflows/ci.yml`.

**Backend** (from `backend/`):

```bash
./gradlew build      # compiles, runs tests, and runs spotlessCheck (format gate)
./gradlew bootRun    # http://localhost:8080
```

**Frontend** (from `frontend/`):

```bash
npm run lint            # ESLint
npm run format:check    # Prettier
npm run typecheck       # tsc -b --noEmit
npm test                # Vitest (jsdom suite)
npm run test:layout     # Playwright layout checks — needs test:layout:install once
npm run build           # tsc -b && vite build
```

`npm run dev:fixtures` serves the API from `frontend/e2e/apiFixtures.ts` instead of proxying to a backend. If you add an endpoint the UI consumes, add its fixture too, or that screen renders an error state in every fixture-backed run.

**Documentation-only changes**: check links and referenced files, and confirm the docs match repository reality.

A third check, **SonarCloud Code Analysis**, reports on pull requests. It comes from the SonarCloud GitHub app, not from a workflow in this repository, so there is no local command for it.

## Required reading order

Before modifying code, read:

1. `docs/architecture-overview.md`
2. `docs/glossary.md`
3. `docs/definition-of-ready.md`
4. `docs/definition-of-done.md`
5. `docs/coding-standards.md`
6. This file's stack and verification sections
7. Relevant ADRs under `docs/adr/` (`ADR-001` .. `ADR-013`)
8. Relevant story spec under `specs/FOR-XXX/` when available
9. `.ai/product.md`, `.ai/architecture.md`, `.ai/domain.md`, `.ai/conventions.md`, `.ai/roadmap.md`

For frontend work also read `docs/ui-guidelines.md`; for endpoints, `docs/api-conventions.md`; for running things locally, `docs/local-development.md`.

## Jira implementation workflow

When asked to implement a Jira story:

1. Resolve the Jira key, for example `FOR-89`.
2. Read `AGENTS.md`.
3. Read all files under `specs/FOR-XXX/` for that story.
4. Read referenced ADRs and global docs.
5. Inspect the repository state before changing files.
6. Create a branch (see Branches and pull requests below).
7. Implement only the requested story.
8. Run the checks from the Verification guidance section.
9. Commit and open a PR.
10. Stop after the PR unless explicitly asked to continue.

Not all work comes from a Jira story. Design changes, fixes and chores follow the same rules minus the spec reading.

## Branches and pull requests

These are the conventions the repository actually uses. They were read off the merged history, not off an older plan.

**Branch**: `<type>/<kebab-description>`, no Jira key.

```text
feat/entrenamiento-sin-scroll
fix/training-week-scoped-sessions
refactor/design-system-buttons
style/quitar-enlace-estadisticas
chore/dev-api-fixtures
docs/agents-md-al-dia
```

Never work directly on `main`.

**Commits and PR titles**: gitmoji + Conventional Commits, description in Spanish.

```text
✨ feat(training): la semana pasa a ser la página, y cabe entera sin scroll
🐛 fix(compra): la lista de compra se puede crear, y se ve
♻️ refactor(design-system): unificar botones, botones de icono y chips
```

`✨ feat` · `🐛 fix` · `♻️ refactor` · `🧪 test` · `📝 docs` · `🔧 chore` · `🚀 perf` · `💄 style` · `🔒 security` · `🗃️ db`

Never add `Co-Authored-By` or AI attribution trailers.

**PR description**: no template file exists; the sections in use are

```markdown
## What changed

## How it was tested

## Known limitations
```

Add `## Lo que deliberadamente NO se mueve` when scope was deliberately limited — reviewers should not have to guess whether an omission was a decision or an oversight. Link the Jira issue when the work came from one. Include screenshots for UI changes.

Prefer squash merge. Keep `main` deployable. Do not merge failing CI unless the failure is unrelated and explicitly documented.

Keep PRs small and reviewable. When a change is unavoidably large, split it into commits that each stand on their own and say in the description which commit carries the decisions.

## Operating rules

- Do not implement code unless the Jira story, spec or user request explicitly asks for implementation.
- Keep changes small, reviewable and tied to one story.
- Preserve hexagonal architecture boundaries.
- Do not place business rules in controllers, UI components or persistence adapters.
- Do not commit secrets, tokens, personal credentials or provider API keys.
- Do not invent requirements when the backlog or spec is silent. Add a note instead.
- Prefer explicit tests over broad unverified changes.
- Update documentation when an architectural decision changes.
- If a story references future functionality that is not present yet, document it as planned instead of creating it early.
- You may read dependency or downstream specs to understand context, but do not implement them unless explicitly requested.

## Architecture principles

- Domain-first design.
- Hexagonal architecture.
- Clear module boundaries.
- Backend owns business rules.
- Frontend consumes read models and commands; it does not duplicate domain logic.
- External providers are adapters, never core domain concepts.

## Quality bar

Every implementation should satisfy:

- Story acceptance criteria.
- Definition of Done.
- Relevant tests/checks from the Verification guidance section.
- No known security regression.
- No hidden coupling between modules.
- Clear error handling.

## Forbidden shortcuts

- Hardcoding user-specific data outside fixtures or seed data.
- Logging sensitive health data or provider tokens.
- Adding wildcard production CORS.
- Bypassing authorization because the MVP is currently single-user.
- Creating speculative abstractions not needed by the current story.
- Claiming a component exists without checking the repository.
- Leaving a comment, message or placeholder that describes a limitation the code no longer has.
