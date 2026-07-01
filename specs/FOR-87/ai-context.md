# AI Context: FOR-87

## Required Reading

1. `AGENTS.md`
2. `docs/coding-standards.md`
3. `docs/definition-of-done.md`
4. `docs/adr/ADR-006.md` or the frontend ADR present in the repo
5. `docs/adr/ADR-007.md` or the testing ADR present in the repo
6. `.ai/conventions.md`

## Agent Instructions

- Set up frontend validation only.
- Keep tests lightweight and useful.
- Show how to test rendering and basic interaction patterns.
- Do not introduce product screen tests before product screens exist.
- Make commands easy for CI and humans to run.

## Expected Output

- Frontend test or type-check command exists.
- Skeleton UI test or equivalent validation exists.
- CI can run frontend validation.

## Risks

- Snapshot-only tests that do not prove behavior.
- Test setup that requires real backend services.
- Frontend tests that encourage duplicating domain logic.
