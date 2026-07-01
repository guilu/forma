# Definition of Ready

A Jira story is ready for implementation when it satisfies this checklist.

## Required sections

Each story must contain:

- Goal
- Business Value
- Spec
- Technical Notes
- Acceptance Criteria
- Definition of Done

## Functional clarity

- The user or system actor is clear.
- The expected behavior is clear.
- Inputs and outputs are described where relevant.
- Important edge cases are mentioned.
- Out-of-scope behavior is called out when there is ambiguity.

## Technical clarity

- Affected bounded context is known.
- Expected layer ownership is clear: domain, application, adapter, API or UI.
- Required integrations are identified.
- Persistence changes are identified if likely.
- Security and authorization expectations are clear.

## Testability

- Acceptance criteria are verifiable.
- Main happy path is testable.
- Main failure or edge cases are testable.
- Required fixtures or sample data are described when needed.

## AI readiness

Before AI-assisted implementation, the story should also have:

- `specs/FOR-XXX/spec.md`
- `specs/FOR-XXX/ai-context.md`
- Additional `api.md`, `ui.md` or `tests.md` files when relevant.

## Not ready if

- The story mixes unrelated features.
- The story requires product decisions not documented anywhere.
- Acceptance criteria describe implementation details instead of outcomes.
- Required security behavior is unknown.
- The scope cannot fit in a reviewable pull request.
