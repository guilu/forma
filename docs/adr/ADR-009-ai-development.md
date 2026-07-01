# ADR-009: AI-Assisted Development

## Status

Accepted

## Context

FORMA is intentionally prepared for development with AI coding agents. Without explicit rules, agents tend to optimize for immediate code output rather than long-term consistency.

## Decision

FORMA will provide repository-level and story-level AI context.

Global context lives in:

- `AGENTS.md`
- `.ai/`
- `docs/`

Story-specific context will live in:

```text
specs/FOR-XXX/
```

## Consequences

- Agents have a clear reading path.
- Repeated context can be centralized.
- Story implementations can be more autonomous and consistent.

## Rules

- Agents must read `AGENTS.md` first.
- Story work must reference the Jira key.
- Agents must not implement out-of-scope features.
- Agents must update tests and docs when behavior changes.
- Agents must preserve architectural boundaries.
- When uncertain, agents should leave explicit notes rather than inventing hidden requirements.
