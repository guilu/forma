# FOR-83: Configure PostgreSQL and migration baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-83
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Configure PostgreSQL access and migration baseline for FORMA.

## Business Value

Provides reliable schema evolution from the beginning instead of relying on manual database changes.

## Scope

Configure persistence foundation only.

The story should include:

- PostgreSQL connection settings.
- Migration tool baseline.
- Initial empty or minimal schema migration.
- Local migration execution.
- Test migration execution where practical.
- Documentation for creating future migrations.

## Architecture Notes

- Align with ADR-003.
- Do not create product tables unless required by the skeleton.
- Migrations must be versioned and committed.
- Configuration must use environment variables or safe defaults.
- Persistence concerns must not leak into domain behavior.

## Acceptance Criteria

- Backend can connect to local PostgreSQL.
- Migration command runs successfully.
- A fresh database can be migrated from scratch.
- Migration naming convention is documented.
- No manual schema setup is required.

## Out of Scope

- Product schema for measurements, training, nutrition, shopping or insights.
- Provider-specific persistence tables.
- Production backup/restore procedures.

## Definition of Done

- Migration tooling configured.
- Baseline migration committed.
- Local verification completed.
- Documentation updated.
