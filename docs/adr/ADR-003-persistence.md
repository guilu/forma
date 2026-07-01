# ADR-003: Persistence

## Status

Accepted

## Context

FORMA needs durable personal data for body measurements, plans, history, integrations, audit events and operational metadata.

## Decision

PostgreSQL is the primary persistence store for the MVP. Schema evolution will be managed through versioned migrations.

The domain model must not depend directly on database tables or ORM annotations unless the chosen implementation explicitly keeps persistence models separate.

## Consequences

- Database schema changes must be migration-driven.
- Tests should run against migrated schema where practical.
- Persistence adapters translate between stored records and domain/application models.
- Backups and restore procedures are part of the MVP platform scope.

## Rules

- Do not use ad-hoc manual schema changes as the normal workflow.
- Avoid destructive migrations unless documented and reviewed.
- Do not let persistence concerns leak into domain behavior.
- Keep timestamps explicit and timezone-aware where relevant.
