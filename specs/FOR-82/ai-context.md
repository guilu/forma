# AI Context: FOR-82

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/coding-standards.md`
4. `docs/adr/ADR-003.md` or the persistence ADR present in the repo
5. `.ai/conventions.md`

## Agent Instructions

- Build a reliable local developer environment, not production infrastructure.
- Keep services minimal and named clearly.
- Use safe example credentials only.
- Make reset/start/stop commands obvious.
- Avoid committing `.env` files that contain real secrets.

## Expected Output

- Developers can start PostgreSQL locally.
- Backend configuration can target the local database.
- Documentation explains start, stop and reset behavior.

## Risks

- Leaking real credentials.
- Overbuilding production-like infrastructure too early.
- Creating service names that drift from docs.
