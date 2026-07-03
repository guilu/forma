# AGENTS.md

This file is the entry point for AI coding agents working on FORMA.

## Project

FORMA is a personal health and fitness planning application. It combines body composition, training, nutrition, shopping, insights and external integrations into one explainable MVP.

Repository: `guilu/forma`
Jira project: `FOR`

## Current repository status

Current phase: Project Bootstrap.

Repository state after FOR-92:

- Documentation foundation exists.
- Story specs exist under `specs/FOR-XXX/` for the bootstrap stories.
- Backend scaffold may not exist until FOR-80 is implemented.
- Frontend scaffold may not exist until FOR-81 is implemented.
- Docker Compose environment may not exist until FOR-82 is implemented.
- PostgreSQL migrations may not exist until FOR-83 is implemented.
- CI may not exist until FOR-84 is implemented.

Always inspect the current repository state before assuming any component exists.

Repository state has priority over roadmap/spec intent. Specs describe the target for a story; the repository describes reality. If they differ, document the gap and do not invent missing code.

## Project stack guidance

Technical stack guidance currently lives here until the stack is split into a dedicated docs file.

Planned bootstrap stack:

- Backend: Java 21, Spring Boot 3.x, build tool selected by FOR-80.
- Frontend: Node.js 24.x or active LTS, React 19.x if compatible, TypeScript, Vite, package manager selected by FOR-81.
- Persistence: PostgreSQL 17.x where practical, migration tool selected by FOR-83.
- Infrastructure: Docker Compose v2 selected by FOR-82.
- CI: GitHub workflow selected by FOR-84.

Do not store technical configuration decisions in `.ai/`. The `.ai/` directory is shared context for agents, not the source of truth for stack versions or executable commands.

## Verification guidance

Run the checks that match the story type and current repository state. If a command does not exist yet, document it as planned instead of inventing it.

| Story type | Expected verification |
|---|---|
| Documentation | Check links and referenced files. Confirm docs match repository reality. |
| Backend | Run backend build and backend tests once FOR-80 defines commands. |
| Frontend | Run frontend build, tests or type checks once FOR-81 defines commands. |
| Infrastructure | Run Docker Compose validation once FOR-82 defines compose files. |
| Persistence | Run migrations against a local database once FOR-83 defines commands. |
| Formatting/linting | Run formatting/linting checks once FOR-85 defines commands. |
| CI | Confirm workflow runs and reports status once FOR-84 defines CI. |

For documentation-only stories in the current docs-only repository, file existence and link checks are valid verification.

## Bootstrap dependency graph

Use this graph to understand context. Reading related specs is allowed; implementing related stories is not allowed unless explicitly requested.

| Story | Depends on | Notes |
|---|---|---|
| FOR-80 | FOR-92 | Backend skeleton can start after specs exist. |
| FOR-81 | FOR-92 | Frontend skeleton can start after specs exist. |
| FOR-82 | FOR-80, FOR-81 where practical | Compose may wire backend/frontend only if they exist. |
| FOR-83 | FOR-80, FOR-82 | PostgreSQL wiring needs backend and local DB service. |
| FOR-84 | FOR-80, FOR-81, FOR-85, FOR-86, FOR-87 where practical | CI should run whatever checks exist at the time. |
| FOR-85 | FOR-80, FOR-81 where practical | Lint/format depends on selected backend/frontend tooling. |
| FOR-86 | FOR-80 | Backend testing baseline depends on backend skeleton. |
| FOR-87 | FOR-81 | Frontend testing baseline depends on frontend skeleton. |
| FOR-88 | FOR-80 | API skeleton depends on backend skeleton. |
| FOR-89 | FOR-80 through FOR-88 as references only | Docs may describe planned commands honestly. |
| FOR-90 | FOR-80, FOR-81 where practical | Environment handling follows selected app structure. |
| FOR-91 | FOR-80, FOR-88 where practical | Logging/correlation works through backend/API baseline. |
| FOR-92 | Documentation foundation | Creates story specs. |
| FOR-93 | FOR-92 and one pilot story | Validates the workflow. |

## Required reading order

Before modifying code, read:

1. `docs/architecture-overview.md`
2. `docs/glossary.md`
3. `docs/definition-of-ready.md`
4. `docs/definition-of-done.md`
5. `docs/coding-standards.md`
6. This file's stack, verification and dependency sections
7. Relevant ADRs under `docs/adr/`
8. Relevant story spec under `specs/FOR-XXX/` when available
9. `.ai/product.md`, `.ai/architecture.md`, `.ai/domain.md`, `.ai/conventions.md`, `.ai/roadmap.md`

## Jira implementation workflow

When asked to implement a Jira story:

1. Resolve the Jira key, for example `FOR-89`.
2. Read `AGENTS.md`.
3. Read all files under `specs/FOR-XXX/` for that story.
4. Read referenced ADRs and global docs.
5. Inspect the repository state before changing files.
6. Create or use a branch named `feature/FOR-XXX-short-description`.
7. Implement only the requested story.
8. Run checks from the Verification guidance section for the story type.
9. Commit and open a PR.
10. Stop after the PR unless explicitly asked to continue.

## Operating rules

- Do not implement code unless the Jira story or spec explicitly asks for implementation.
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

## Pull request expectations

PR titles should start with the Jira key when applicable.

Use this structure in PR descriptions:

```markdown
## What changed

## How it was tested

## Known limitations

## Jira

https://dbhlab.atlassian.net/browse/FOR-XXX
```

Stop after opening the PR unless explicitly asked to continue with another story.
