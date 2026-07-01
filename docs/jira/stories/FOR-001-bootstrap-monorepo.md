# FOR-001 Bootstrap monorepo

## Epic

Foundation

## Goal

Create the initial Forma monorepo skeleton.

## Business value

All future development needs a stable structure for backend, frontend, infrastructure and documentation.

## Technical notes

- Backend: Java 21 + Spring Boot 3
- Frontend: React + TypeScript + Vite + TailwindCSS
- Infra: Docker Compose + PostgreSQL
- Follow docs/architecture.md and docs/ui-guidelines.md

## Acceptance criteria

- [ ] `backend/` project exists and builds.
- [ ] `frontend/` project exists and builds.
- [ ] `infra/docker-compose.yml` starts PostgreSQL.
- [ ] README explains local development.
- [ ] Basic CI workflow exists.

## Definition of Done

- [ ] CI green.
- [ ] Documentation updated.
- [ ] PR references FOR-001.
