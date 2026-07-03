# AI Context: FOR-91

## Required Reading

1. `AGENTS.md`
2. `docs/coding-standards.md`
3. `docs/definition-of-done.md`
4. `docs/adr/ADR-008.md` or the observability/logging ADR present in the repo
5. `.ai/architecture.md`
6. `.ai/conventions.md`

## Agent Instructions

- Add minimal structured logging and correlation ID support.
- Ensure correlation IDs appear in request logs and errors where practical.
- Never log secrets, provider tokens or full personal health payloads.
- Keep local logs readable.
- Do not add a full observability stack in this story.

## Expected Output

- Requests receive or propagate a correlation ID.
- Logs include the correlation ID.
- Sensitive logging rules are documented.
- Smoke/API requests are diagnosable.

## Risks

- Logging too much personal data.
- Creating logging wrappers that obscure normal framework behavior.
- Adding heavyweight tracing before the baseline exists.
