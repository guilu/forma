# AI Context: FOR-80

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/glossary.md`
4. `docs/definition-of-ready.md`
5. `docs/definition-of-done.md`
6. `docs/coding-standards.md`
7. `docs/adr/ADR-001.md` or the architecture ADR present in the repo
8. `.ai/architecture.md`
9. `.ai/conventions.md`

## Agent Instructions

- Create foundation only; do not implement product behavior.
- Keep domain packages framework-free.
- Keep controllers thin if a smoke or health-adjacent endpoint is added.
- Prefer boring, conventional project structure over clever abstractions.
- Add only files needed for a running backend baseline.

## Expected Output

- Backend can build and start locally.
- Backend structure is clear enough for later stories.
- A future agent can identify where domain, application, adapter and delivery code belongs.

## Risks

- Overengineering the module structure before product code exists.
- Mixing Spring/framework concerns into domain packages.
- Adding placeholder features that look like product behavior.
