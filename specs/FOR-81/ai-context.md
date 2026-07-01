# AI Context: FOR-81

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/coding-standards.md`
4. `docs/definition-of-done.md`
5. `docs/adr/ADR-006.md` or the frontend ADR present in the repo
6. `.ai/product.md`
7. `.ai/architecture.md`
8. `.ai/conventions.md`

## Agent Instructions

- Build the frontend foundation only.
- Do not implement product screens disguised as placeholders.
- Keep API client behavior centralized even if it is only a placeholder.
- Set up route/layout structure that can grow without forcing future rewrites.
- Keep domain calculations out of UI code.

## Expected Output

- Frontend starts locally.
- Basic route renders.
- Test or type-check command exists.
- Future UI stories have an obvious place to add pages/components.

## Risks

- Hardcoding future navigation before stories exist.
- Duplicating backend domain logic in frontend helpers.
- Creating a design system prematurely.
