# AGENTS.md

This file is the entry point for AI coding agents working on FORMA.

## Project

FORMA is a personal health and fitness planning application. It combines body composition, training, nutrition, shopping, insights and external integrations into one explainable MVP.

Repository: `guilu/forma`
Jira project: `FOR`

## Required reading order

Before modifying code, read:

1. `docs/architecture-overview.md`
2. `docs/glossary.md`
3. `docs/definition-of-ready.md`
4. `docs/definition-of-done.md`
5. `docs/coding-standards.md`
6. Relevant ADRs under `docs/adr/`
7. Relevant story spec under `specs/FOR-XXX/` when available
8. `.ai/product.md`, `.ai/architecture.md`, `.ai/domain.md`, `.ai/conventions.md`, `.ai/roadmap.md`

## Operating rules

- Do not implement code unless the Jira story or spec explicitly asks for implementation.
- Keep changes small, reviewable and tied to one story.
- Preserve hexagonal architecture boundaries.
- Do not place business rules in controllers, UI components or persistence adapters.
- Do not commit secrets, tokens, personal credentials or provider API keys.
- Do not invent requirements when the backlog or spec is silent. Add a note instead.
- Prefer explicit tests over broad unverified changes.
- Update documentation when an architectural decision changes.

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
- Relevant tests.
- No known security regression.
- No hidden coupling between modules.
- Clear error handling.

## Forbidden shortcuts

- Hardcoding user-specific data outside fixtures or seed data.
- Logging sensitive health data or provider tokens.
- Adding wildcard production CORS.
- Bypassing authorization because the MVP is currently single-user.
- Creating speculative abstractions not needed by the current story.

## Commit and PR expectations

- Branch names should reference Jira keys when implementing stories: `feature/FOR-123-short-description`.
- PR titles should start with the Jira key when applicable.
- PR descriptions should include what changed, how it was tested and any known limitations.
