# AI Context: FOR-84

## Required Reading

1. `AGENTS.md`
2. `docs/definition-of-done.md`
3. `docs/coding-standards.md`
4. `docs/adr/ADR-007.md` or the testing ADR present in the repo
5. `.ai/conventions.md`

## Agent Instructions

- Configure CI for the current project shape only.
- Prefer one clear workflow over several half-configured workflows.
- Reuse the same commands developers run locally.
- Keep dependency caches safe and conventional.
- Do not add deployment behavior.

## Expected Output

- PRs show CI status.
- Main updates trigger CI.
- Backend and frontend failures fail the pipeline.

## Risks

- CI commands drift from documented local commands.
- Workflow is too slow for small PRs.
- Pipeline silently skips meaningful checks.
