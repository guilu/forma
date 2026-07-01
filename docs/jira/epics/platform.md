# FOR-EPIC Platform & Infrastructure

## Goal

Provide the technical infrastructure needed to run, test and deploy Forma reliably.

## Business value

A predictable platform lets agents ship changes safely without turning every PR into una gymkana con casco.

## Scope

- Docker Compose
- PostgreSQL
- Flyway
- GitHub Actions
- deployment to forma.diegobarrioh.dev
- environment configuration
- observability basics later

## Initial stories

- FOR-070 Docker Compose environment
- FOR-071 GitHub Actions CI
- FOR-072 Environment configuration
- FOR-073 Deployment target setup
- FOR-074 Basic logs and health checks

## Acceptance criteria

- Local environment is reproducible.
- CI validates changes.
- Deployment setup is documented.
- App exposes basic health checks.
