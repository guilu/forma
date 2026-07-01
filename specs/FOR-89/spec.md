# FOR-89: Create local development documentation

Jira: https://dbhlab.atlassian.net/browse/FOR-89
Story points: 1
Epic: FOR-79 Project Bootstrap

## Goal

Create local development documentation for FORMA.

## Business Value

Allows humans and AI agents to start the project consistently without rediscovering commands or environment assumptions.

## Scope

Document the current local development workflow.

Documentation should cover:

- Required tools and versions.
- Backend startup.
- Frontend startup.
- Docker Compose startup.
- Database migration command.
- Test commands.
- Lint/format commands.
- Troubleshooting.
- Local reset procedure.

## Architecture Notes

- Align with existing docs and `AGENTS.md`.
- Commands should be copy-pasteable.
- Avoid aspirational commands that do not work yet.
- Update this document as bootstrap stories land.

## Acceptance Criteria

- A developer can follow the docs from a clean checkout.
- Backend startup is documented.
- Frontend startup is documented.
- Infrastructure startup is documented.
- Common failure cases are documented.

## Out of Scope

- Production operations guide.
- Cloud deployment documentation.
- Full contributor handbook.

## Definition of Done

- Local development docs committed through PR.
- Commands verified where practical.
- README links to the document if appropriate.
- Known limitations documented.
