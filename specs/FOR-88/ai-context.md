# AI Context: FOR-88

## Required Reading

1. `AGENTS.md`
2. `docs/architecture-overview.md`
3. `docs/coding-standards.md`
4. `docs/adr/ADR-005.md` or the REST API ADR present in the repo
5. `.ai/architecture.md`
6. `.ai/conventions.md`

## Agent Instructions

- Create API conventions only.
- Do not implement product endpoints.
- Define one safe, consistent error shape.
- Keep validation and error handling safe for clients and useful for debugging.
- Ensure stack traces and internals are not exposed.

## Expected Output

- API base path convention exists.
- Error response baseline exists.
- Validation error strategy is prepared.
- Basic API smoke behavior can be tested.

## Risks

- Treating this story as permission to create product routes.
- Returning persistence entities or internal exceptions from API responses.
- Making errors opaque enough that debugging becomes archaeology.
