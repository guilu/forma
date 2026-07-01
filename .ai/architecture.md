# FORMA AI Architecture Context

## Core architecture

FORMA uses hexagonal architecture with DDD principles.

## Dependency rule

Dependencies point inward.

```text
Delivery / Adapters -> Application -> Domain
```

## Layer ownership

### Domain

Owns:

- Entities
- Value objects
- Domain services
- Business rules

Must not depend on:

- Spring
- React
- PostgreSQL
- Provider SDKs
- HTTP clients

### Application

Owns:

- Use cases
- Commands
- Queries
- Ports
- Transaction/application orchestration

### Adapters

Own:

- Database persistence
- External provider APIs
- Scheduling
- Metrics/logging glue
- Security framework integration

### Delivery

Owns:

- REST controllers
- API DTOs
- Frontend pages/components

## AI warning

Do not collapse layers to make a story faster. That is how small apps become expensive archaeology.
