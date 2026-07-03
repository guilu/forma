# AI Context: FOR-90

## Required Reading

1. `AGENTS.md`
2. `docs/coding-standards.md`
3. `docs/definition-of-done.md`
4. `docs/adr/ADR-002.md` or the authentication/authorization ADR present in the repo
5. `.ai/conventions.md`

## Agent Instructions

- Treat secrets as hostile until proven fake.
- Commit only example files with fake values.
- Ensure local secret files are ignored.
- Keep required variables documented in one obvious place.
- Fail clearly for missing critical backend configuration.

## Expected Output

- Safe environment variable baseline.
- Example files with no real secrets.
- Documented secret handling rules.
- Backend configuration can load from environment.

## Risks

- Accidentally committing real tokens or local secrets.
- Creating frontend-exposed variables for backend-only secrets.
- Silent fallback to unsafe defaults.
