# FORMA AI Conventions

## Naming

Use names from `docs/glossary.md`.

## Jira

- Every story maps to a `FOR-XXX` Jira issue.
- Story specs will live under `specs/FOR-XXX/`.
- Branches should include the Jira key when implementing code.

## Backend

- Keep controllers thin.
- Keep use cases explicit.
- Keep adapters replaceable.
- Do not expose persistence entities as API responses.

## Frontend

- Use shared components for common UI patterns.
- Do not duplicate domain calculations.
- Handle loading, empty and error states.

## Tests

- Add tests where behavior changes.
- Keep tests at the lowest practical layer.
- Use fixtures deliberately.

## Documentation

- Update ADRs for architecture changes.
- Update glossary when introducing domain terms.
- Update specs when implementation clarifies behavior.
