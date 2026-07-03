# FORMA Story Specs

Story-specific implementation guidance for AI-assisted development.

## Structure

Each Jira story should use:

```text
specs/FOR-XXX/
  spec.md
  ai-context.md
  tests.md
  api.md        # when API/backend contract guidance is useful
  ui.md         # when frontend guidance is useful
```

## Usage

Before implementing a Jira story, agents should read:

1. `AGENTS.md`
2. Global docs under `docs/`
3. Shared AI context under `.ai/`
4. The story-specific folder under `specs/FOR-XXX/`

## Current Bootstrap Specs

- `FOR-80`: backend application skeleton
- `FOR-81`: frontend application skeleton
- `FOR-82`: local Docker Compose environment
- `FOR-83`: PostgreSQL and migration baseline
- `FOR-84`: CI quality gate
- `FOR-85`: formatting and linting baseline
- `FOR-86`: backend testing baseline
- `FOR-87`: frontend testing baseline
- `FOR-88`: API skeleton and error response baseline
- `FOR-89`: local development documentation
- `FOR-90`: secrets and environment variable baseline
- `FOR-91`: structured logging and correlation ID baseline
- `FOR-92`: bootstrap story specs
