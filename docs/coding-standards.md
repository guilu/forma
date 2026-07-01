# Coding Standards

## General principles

- Prefer clear, boring code over clever code.
- Keep changes small and tied to one Jira story.
- Use names from `docs/glossary.md`.
- Put business rules in domain/application code, not in UI, controllers or persistence adapters.
- Do not commit secrets.

## Backend

### Architecture

- Organize code by bounded context where possible.
- Keep domain code free from infrastructure dependencies.
- Application services orchestrate use cases.
- Ports are owned by the application/domain side.
- Adapters implement ports.

### Java/Spring conventions

- Prefer constructor injection.
- Avoid field injection.
- Keep controllers thin.
- Validate input at API boundaries.
- Use explicit DTOs for API contracts.
- Avoid exposing persistence entities directly from controllers.

### Errors

- Use consistent error responses.
- Do not expose stack traces to users.
- Include correlation IDs in logs.
- Log enough context to diagnose, but never secrets or full personal health payloads.

## Frontend

- Keep domain calculations out of components.
- Consume backend read models.
- Centralize API client behavior.
- Use reusable components for cards, forms, status badges and states.
- Every page should support loading, empty and error states.
- Forms must show validation errors near the relevant fields.

## CSS/design

- Prefer design tokens over hardcoded values.
- Keep light/dark mode support token-based.
- Avoid one-off visual rules inside feature pages.

## Testing

- Domain rules require domain tests.
- Use case behavior requires application tests.
- API behavior requires request/response tests where practical.
- UI behavior requires component or interaction tests for important states.
- Bug fixes should include regression tests.

## Documentation

- Update docs when behavior or architecture changes.
- Add ADRs for important decisions.
- Keep examples executable or clearly marked as illustrative.
