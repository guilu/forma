# AI Context: FOR-92

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/definition-of-ready.md`
4. `docs/definition-of-done.md`
5. `docs/coding-standards.md`
6. `.ai/product.md`
7. `.ai/architecture.md`
8. `.ai/domain.md`
9. `.ai/conventions.md`
10. `.ai/roadmap.md`

## Agent Instructions

- Create documentation/spec files only.
- Do not modify application code.
- Use one consistent structure across bootstrap stories.
- Keep specs implementation-oriented, not essay-like.
- Add `api.md` only for API/backend contract guidance.
- Add `ui.md` only for frontend/UI guidance.
- Preserve Jira story intent; do not invent new product scope.

## Expected Output

- `specs/FOR-80` through `specs/FOR-92` exist.
- Each folder includes `spec.md`, `ai-context.md` and `tests.md`.
- API/UI-specific files exist only where useful.
- Future coding agents can implement one Jira at a time.

## Known Constraint

The Jira description mentions templates under `templates/`, but no `templates/spec.md` was found in the repository during execution. The generated structure should therefore follow the existing documentation model from `AGENTS.md`, `.ai/roadmap.md` and `docs/definition-of-ready.md`.

## Risks

- Over-documenting until specs become decorative bureaucracy.
- Accidentally implementing code while preparing implementation guidance.
- Creating generic specs that do not help agents make concrete decisions.
