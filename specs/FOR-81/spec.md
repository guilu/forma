# FOR-81: Initialize frontend application skeleton

Jira: https://dbhlab.atlassian.net/browse/FOR-81
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Create the frontend application skeleton for FORMA.

## Business Value

Provides the UI foundation required to implement MVP screens consistently and incrementally.

## Scope

Implement only the frontend skeleton. Do not implement product UI features.

The skeleton should include:

- Build configuration.
- Application entry point.
- Basic routing setup.
- Initial layout placeholder.
- API client placeholder.
- Initial test execution support.

## Architecture Notes

- Follow `AGENTS.md` and `docs/adr/ADR-006.md` or the frontend ADR present in the repo.
- Frontend renders state and collects commands; it does not own domain rules.
- Prepare for responsive layout and design system stories without hardcoding future screens.

## Acceptance Criteria

- Frontend project builds successfully.
- Frontend application can start locally.
- Basic route renders correctly.
- At least one basic test or type check runs successfully.
- No user-facing product feature is implemented.

## Out of Scope

- Real dashboard, body, training, nutrition, shopping or insights screens.
- Authentication UI.
- Final design system implementation.

## Definition of Done

- Frontend skeleton committed through PR.
- Local startup verified.
- Build/test command documented or obvious.
- CI-ready project structure exists.
