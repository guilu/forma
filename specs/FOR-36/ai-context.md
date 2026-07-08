# FOR-36 AI Context

## Story

FOR-36 — Create shopping product API
(https://dbhlab.atlassian.net/browse/FOR-36)

## Intent

Let the user maintain products (names, links, estimated prices) without touching
code. Success is a persisted CRUD API (list/create/update) over FOR-35
`ShoppingProduct`, with the standard error contract and no external price sync.

## Relevant Documents

- `AGENTS.md`
- `docs/api-conventions.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-003-persistence.md`,
  `docs/adr/ADR-007-testing.md`
- `docs/api/body-measurements.md` (endpoint + error precedent)
- `specs/FOR-35/` (the model), `specs/FOR-16/`, `specs/FOR-17/` (persistence +
  API precedents)
- Jira: https://dbhlab.atlassian.net/browse/FOR-36

## Domain Notes

- `ShoppingProduct` is user data (editable, persisted) — unlike the in-code
  reference catalogs (exercises, foods). This is the first persisted mutable
  Shopping entity.
- Prices are editable estimates; no automatic external pricing.

## Architectural Constraints

- Persistence via JDBC + Flyway (no ORM). New migration after
  `V3__training_session_status.sql` (verify next free version; never edit
  existing migrations — ADR-003). `NUMERIC` for money.
- Hexagonal layering: repository **port** in application, JDBC **adapter** in
  `adapter/persistence/` (FOR-16 pattern); thin controller in `delivery/`
  (FOR-17 pattern) reusing `ApiPaths.V1` and the FOR-88/FOR-27 error handler.
- DTOs distinct from domain/persistence types (ADR-005).

## Common Pitfalls

- Returning the domain type or persistence row directly from the controller.
- Floating-point money — use `BigDecimal`/`NUMERIC`.
- Adding `DELETE` (out of scope) or external price sync.
- Editing an existing migration instead of adding a new one.

## Suggested Implementation Order

1. Flyway migration for the products table (NUMERIC prices).
2. Repository port + JDBC adapter (list/save/update/find-by-id).
3. Thin controller + request/response DTOs + validation; wire 404/400 to the
   existing handler.
4. Controller tests (`@WebMvcTest`) + a repository integration test (H2).

## Validation

Run `./gradlew test` from `backend/` (AGENTS.md "Backend"/"Persistence" rows).
