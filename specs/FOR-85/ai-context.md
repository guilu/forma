# AI Context: FOR-85

## Required Reading

1. `AGENTS.md`
2. `docs/coding-standards.md`
3. `docs/definition-of-done.md`
4. `.ai/conventions.md`

## Agent Instructions

- Use standard formatting/linting tools for the selected backend/frontend stack.
- Keep commands simple and documented.
- Avoid custom scripts unless they genuinely reduce ambiguity.
- Do not perform unrelated code rewrites.
- Wire checks into CI only where practical in this story.

## Expected Output

- Developers and agents have one obvious way to check formatting/linting.
- Formatting rules are reproducible across machines.
- Future PRs have less style noise.

## Risks

- Tooling choice fights the selected stack.
- Formatting churn hides real changes.
- Rules become more important than product work. Clásico síndrome del linter con complejo de arquitecto.
