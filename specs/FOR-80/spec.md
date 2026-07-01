# FOR-80: Initialize backend application skeleton

Jira: https://dbhlab.atlassian.net/browse/FOR-80
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Create the backend application skeleton for FORMA.

## Business Value

Provides the technical foundation required to implement domain and API stories consistently.

## Scope

Implement only the backend skeleton. Do not implement product domain behavior.

The skeleton should include:

- Build configuration.
- Application entry point.
- Basic package/module structure.
- Environment-based configuration baseline.
- Health placeholder endpoint if appropriate.
- Initial test execution support.

## Architecture Notes

- Follow `AGENTS.md` before making code changes.
- Follow `docs/architecture-overview.md` and ADR-001.
- Preserve hexagonal architecture boundaries from the start.
- Keep framework code out of the domain package.
- Do not add speculative modules beyond the MVP bounded contexts.

## Acceptance Criteria

- Backend project builds successfully.
- Backend application can start locally.
- Initial package/module structure is present.
- At least one basic test runs successfully.
- No user-facing domain feature is implemented.

## Out of Scope

- Real body composition, training, nutrition, shopping, insights or integration behavior.
- Authentication implementation unless required by the selected skeleton.
- Production deployment configuration.

## Definition of Done

- Backend skeleton committed through PR.
- Local startup verified.
- Build/test command documented or obvious.
- CI-ready project structure exists.
