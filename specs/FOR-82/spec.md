# FOR-82: Create local Docker Compose environment

Jira: https://dbhlab.atlassian.net/browse/FOR-82
Story points: 2
Epic: FOR-79 Project Bootstrap

## Goal

Create a local Docker Compose environment for FORMA.

## Business Value

Allows developers and AI agents to run required infrastructure consistently without guessing local services.

## Scope

Create the first local infrastructure baseline.

Docker Compose should provide:

- PostgreSQL service.
- Optional backend service wiring if practical.
- Optional frontend service wiring if practical.
- Named volumes for local persistence.
- Environment variable examples.
- Simple start/stop commands documented.

## Architecture Notes

- Align service names with documentation.
- Prefer local development reliability over production completeness.
- Do not commit real secrets.
- Keep this minimal; Kubernetes, production hosting and backups are separate concerns.

## Acceptance Criteria

- Docker Compose starts required local services.
- PostgreSQL is reachable from backend configuration.
- Local data persists across restarts unless explicitly reset.
- Required environment variables are documented.
- No real secrets are committed.

## Out of Scope

- Production deployment.
- Cloud infrastructure.
- Full observability stack.

## Definition of Done

- Compose file committed through PR.
- Local startup verified.
- Reset procedure documented.
- Documentation updated.
