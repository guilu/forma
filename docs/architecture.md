# Forma architecture

## Architecture style

Forma should start as a pragmatic modular monorepo:

```txt
forma/
  backend/
  frontend/
  infra/
  docs/
```

The backend should use a clean modular structure inspired by hexagonal architecture, but without excessive ceremony for the MVP.

## Backend

Recommended stack:

- Java 21
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security later, not mandatory for first local-only prototype
- Testcontainers for integration tests

Suggested package structure:

```txt
com.diegobarrioh.forma
  body/
    domain/
    application/
    infrastructure/
    api/
  training/
    domain/
    application/
    infrastructure/
    api/
  nutrition/
    domain/
    application/
    infrastructure/
    api/
  shopping/
    domain/
    application/
    infrastructure/
    api/
  insights/
    domain/
    application/
    api/
  integrations/
    withings/
```

## Frontend

Recommended stack:

- React
- TypeScript
- Vite
- TailwindCSS
- Recharts
- TanStack Query
- React Router

Suggested structure:

```txt
frontend/src/
  app/
  components/
  features/
    dashboard/
    body/
    training/
    nutrition/
    shopping/
    insights/
    integrations/
  lib/
```

## Database

PostgreSQL should be the default database. H2 is not recommended except maybe for lightweight tests. The local development environment should behave as close as possible to production.

Initial schema areas:

- body_measurements
- running_sessions
- running_plan_sessions
- exercises
- strength_workouts
- strength_workout_items
- meal_templates
- food_items
- mercadona_products
- shopping_items
- weekly_checkins
- recommendations

## Integration boundaries

### Withings

The Withings integration should be prepared but not implemented in the MVP. Keep it behind an integration port/service so the rest of the app does not depend on Withings-specific payloads.

Planned behaviour:

1. User connects Withings via OAuth2.
2. App stores access token and refresh token securely.
3. App imports measurements periodically.
4. Imported data is normalized into `BodyMeasurement`.

### Mercadona

Mercadona should not be treated as a live external API in MVP. Use an editable product catalog instead:

- product name
- product URL
- package size
- unit price
- cost per serving
- last checked date

## Deployment direction

Initial deployment can be a single Docker Compose stack:

```txt
reverse-proxy -> frontend static build
reverse-proxy -> backend API
backend -> postgres
```

Target subdomain:

```txt
forma.diegobarrioh.dev
```

## API style

REST is enough for MVP.

Suggested endpoints:

```txt
GET    /api/dashboard
GET    /api/body/measurements
POST   /api/body/measurements
GET    /api/training/running-plan
GET    /api/training/strength-plan
GET    /api/nutrition/day-templates
GET    /api/shopping/products
POST   /api/shopping/products
PUT    /api/shopping/products/{id}
GET    /api/insights/weekly
```

## Testing strategy

Backend:

- domain unit tests
- application service tests
- repository integration tests with Testcontainers
- controller tests for API contracts

Frontend:

- component tests for core widgets later
- keep first MVP simple and manually testable

## First technical milestone

The first technical PR should create:

1. `backend/` Spring Boot project.
2. `frontend/` Vite React project.
3. `infra/docker-compose.yml` with PostgreSQL.
4. Root README instructions.
5. Basic CI running backend and frontend checks.
