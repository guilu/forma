# Forma

Personal fitness dashboard for Diego: body metrics, running, strength training, nutrition planning and shopping budget.

## Product idea

Forma starts as the web-app migration of the current Excel-based fitness plan. The goal is not to build another generic habit tracker, but a personal operating system for training and nutrition.

It should help answer questions such as:

- Am I improving my body composition or just losing weight?
- Are running performance, recovery and strength moving in the right direction?
- Am I eating enough protein without making dinner too heavy after late summer runs?
- What does my weekly nutrition plan cost?
- Which Mercadona products are part of my default shopping list?

## Target domain

```txt
forma.diegobarrioh.dev
```

## Proposed stack

```txt
frontend/  React + TypeScript + Vite + TailwindCSS + Recharts
backend/   Java 21 + Spring Boot 3 + PostgreSQL + Flyway
infra/     Docker Compose first, deploy later behind reverse proxy
```

## First milestone

Build an MVP with manual data entry before integrating Withings.

MVP scope:

1. Dashboard with current metrics and trends.
2. Manual body measurement entry.
3. Seeded running plan from 4 km to 10 km.
4. Seeded strength plan for dumbbells, bench, bands and pull-up bar.
5. Nutrition templates by day type: running, strength and rest.
6. Shopping budget with editable Mercadona products.
7. Docker Compose local environment.

## Documentation

- [Local development](docs/local-development.md)
- [API conventions](docs/api-conventions.md)
- [Product vision](docs/vision.md)
- [Roadmap](docs/roadmap.md)
- [Architecture](docs/architecture.md)
- [Domain model](docs/domain-model.md)
- [UI guidelines](docs/ui-guidelines.md)
- [Codex prompts](docs/prompts/codex.md)
