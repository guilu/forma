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

## Canonical technical references

- Runtime/tool versions: `docs/stack.md`
- Verification/check commands by story type: `docs/verification-checks.md`
- Bootstrap dependency graph: `docs/story-dependency-graph.md`

Do not store technical configuration decisions in `.ai/`. The `.ai/` directory is shared context for agents, not the source of truth for stack versions or executable commands.

## Required reading order

Before modifying code, read:

1. `docs/architecture-overview.md`
2. `docs/glossary.md`
3. `docs/definition-of-ready.md`
4. `docs/definition-of-done.md`
5. `docs/coding-standards.md`
6. `docs/stack.md`
7. `docs/verification-checks.md`
8. `docs/story-dependency-graph.md`
9. Relevant ADRs under `docs/adr/`
10. Relevant story spec under `specs/FOR-XXX/` when available
11. `.ai/product.md`, `.ai/architecture.md`, `.ai/domain.md`, `.ai/conventions.md`, `.ai/roadmap.md`

## Jira implementation workflow

When asked to implement a Jira story:

1. Resolve the Jira key, for example `FOR-89`.
2. Read `AGENTS.md`.
3. Read all files under `specs/FOR-XXX/` for that story.
4. Read referenced ADRs and global docs.
5. Inspect the repository state before changing files.
6. Create or use a branch named `feature/FOR-XXX-short-description`.
7. Implement only the requested story.
8. Run checks listed in `docs/verification-checks.md` for the story type.
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
- Relevant tests/checks from `docs/verification-checks.md`.
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

## Commit and PR expectations

- Branch names should reference Jira keys when implementing stories: `feature/FOR-123-short-description`.
- PR titles should start with the Jira key when applicable.
- PR descriptions should include what changed, how it was tested and any known limitations.
- Use `.github/pull_request_template.md` when opening PRs.
