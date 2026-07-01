# AI Context: FOR-83

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/coding-standards.md`
4. `docs/adr/ADR-003.md` or the persistence ADR present in the repo
5. `.ai/architecture.md`
6. `.ai/conventions.md`

## Agent Instructions

- Configure database access and migrations only.
- Keep migration setup boring and standard.
- Do not introduce product tables early.
- Keep configuration environment-driven.
- Avoid manual database setup instructions except as troubleshooting notes.

## Expected Output

- A fresh local database can be migrated from scratch.
- Backend can connect to PostgreSQL.
- Future stories have a clear migration naming convention.

## Risks

- Creating schema for future features before their stories.
- Mixing persistence model concerns into domain packages.
- Making local setup depend on hidden local machine state.
