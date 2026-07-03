# FOR-91: Add basic structured logging and correlation ID baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-91
Story points: 5
Epic: FOR-79 Project Bootstrap

## Goal

Add basic structured logging and correlation ID baseline for FORMA.

## Business Value

Makes early backend behavior diagnosable and prepares the platform for later observability stories.

## Scope

Create the first backend logging/correlation baseline.

The baseline should include:

- Request correlation ID generation.
- Correlation ID propagation in logs.
- Basic request logging.
- Safe error logging rules.
- Logging configuration for local development.
- Documentation of sensitive logging constraints.

## Architecture Notes

- Align with ADR-008 and FOR-68 if present.
- Keep implementation minimal.
- Do not log full health payloads, tokens or secrets.
- Correlation ID should exist even for smoke endpoints.
- Logs must help debugging without exposing personal health data.

## Acceptance Criteria

- Backend requests receive or generate correlation IDs.
- Logs include correlation IDs.
- Sensitive values are not logged.
- Local logs are readable during development.
- Logging behavior is documented.

## Out of Scope

- Full metrics platform.
- Distributed tracing backend.
- Audit event domain model unless already required by another story.

## Definition of Done

- Logging baseline committed through PR.
- Basic request logging verified.
- Sensitive logging constraints documented.
- Tests or manual verification completed.
