# FOR-17 AI Context

## Story

FOR-17 — Create body measurements API
(https://dbhlab.atlassian.net/browse/FOR-17)

## Intent

Give the frontend (FOR-18/FOR-19/FOR-20) a stable, versioned contract for
reading and writing body measurements, built on FOR-15 (domain) and FOR-16
(persistence). Success is a controller that validates input, never leaks
persistence/domain types, and returns predictable errors.

## Relevant Documents

- `AGENTS.md`
- `docs/api-conventions.md`
- `docs/adr/ADR-005-api-design.md`
- `docs/adr/ADR-001-architecture.md`
- `docs/adr/ADR-007-testing.md`
- `specs/FOR-15/`, `specs/FOR-16/` (domain model and persistence this API
  sits on)
- Jira: https://dbhlab.atlassian.net/browse/FOR-17

## Domain Notes

The API must not reimplement the FOR-15 derived-value calculation
(`fatMassKg`/`leanMassKg`) — it reads whatever the domain/repository layer
already computed or derived.

## Architectural Constraints

- Controllers live under
  `backend/src/main/java/dev/diegobarrioh/forma/delivery/`, alongside the
  existing `ApiPaths`, `PingController` and `error/` package.
- Mount under `ApiPaths.V1`; do not hardcode `/api/v1` inline.
- Reuse the existing `ApiError`/`ApiErrorCode`/`GlobalExceptionHandler`
  baseline (FOR-88) rather than inventing a new error shape.
- Controllers stay thin: validation + DTO mapping only, no business rules
  (ADR-001, ADR-005).

## Common Pitfalls

- Returning the FOR-15 domain object or the FOR-16 persistence row directly
  instead of a response DTO.
- Accepting `source` from the client on `POST` — it must always be `MANUAL`
  here.
- Introducing a new error response shape instead of the existing `ApiError`.
- Forgetting the `/api/v1` prefix and hardcoding `/api/body/measurements` as
  written in the raw Jira text (see spec.md's explicit note on this).

## Suggested Implementation Order

1. Define request/response DTOs.
2. Add validation annotations (Bean Validation, per `spring-boot-starter-validation`
   already in `backend/build.gradle`).
3. Implement the controller calling into the FOR-16 repository (directly or
   through a thin application use case).
4. Add controller tests (`@WebMvcTest`-style, following
   `backend/src/test/java/dev/diegobarrioh/forma/delivery/PingControllerTest.java`
   and `GlobalExceptionHandlerTest.java` patterns).
5. Document the endpoints in the README/docs per the story's DoD.

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md Verification guidance,
"Backend" row — the API skeleton and error baseline from FOR-88 already
exist in the repo).
