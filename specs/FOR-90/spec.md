# FOR-90: Configure secrets and environment variable baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-90
Story points: 2
Epic: FOR-79 Project Bootstrap

## Goal

Configure the secrets and environment variable baseline for FORMA.

## Business Value

Prevents accidental credential leaks and gives local, CI and future production environments a predictable configuration model.

## Scope

Create configuration and secret-handling baseline.

The baseline should include:

- Example environment files without real secrets.
- Required variable documentation.
- Backend configuration loading.
- Frontend configuration loading where needed.
- Secret handling rules.
- Git ignore rules for local secret files.

## Architecture Notes

- Align with ADR-002 and FOR-66 if present.
- No real secrets may be committed.
- Fail fast when critical backend configuration is missing.
- Keep local defaults safe and obvious.

## Acceptance Criteria

- Example env files exist without secrets.
- Local secret files are ignored.
- Required variables are documented.
- Backend can load configuration from environment.
- Missing critical config fails clearly where applicable.

## Out of Scope

- Secret manager integration.
- Production credential provisioning.
- Provider OAuth credentials beyond placeholders/examples.

## Definition of Done

- Configuration baseline committed through PR.
- Secret safety reviewed.
- Local startup verified.
- Documentation updated.
