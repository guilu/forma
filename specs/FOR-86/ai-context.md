# AI Context: FOR-86

## Required Reading

1. `AGENTS.md`
2. `docs/coding-standards.md`
3. `docs/definition-of-done.md`
4. `docs/adr/ADR-007.md` or the testing ADR present in the repo
5. `.ai/architecture.md`
6. `.ai/conventions.md`

## Agent Instructions

- Create backend testing foundations only.
- Keep examples small and meaningful.
- Prefer tests that show future agents where each layer should be tested.
- Keep domain tests free from Spring/framework dependencies.
- Document naming conventions and commands.

## Expected Output

- Backend test command exists.
- At least one skeleton unit test exists.
- At least one application/API-level skeleton test exists where practical.
- CI can run backend tests.

## Risks

- Creating elaborate test infrastructure without product behavior.
- Testing framework bootstrapping instead of behavior.
- Mocking layers in ways that encourage bad future tests.
