# FOR-36: Create shopping product API

Jira: https://dbhlab.atlassian.net/browse/FOR-36
Epic: FOR-5 Shopping Assistant

## Summary

Persist `ShoppingProduct` (FOR-35) and expose a CRUD API to list, create and
update products, including the optional link to a FOR-30 food. This is the first
**user-editable, persisted** entity in Shopping (unlike the Training/Nutrition
in-code catalogs), so it introduces a Flyway table, a repository port + JDBC
adapter, and DTOs — following the FOR-16/FOR-17 body precedents.

## User/System Flow

1. Client lists products (`GET`), creates one (`POST`), or updates one (`PUT`).
2. The controller validates the request DTO, the application/repository persists
   it, and the response returns the product (never the persistence row).
3. FOR-37 list items and FOR-38 budget read these products.

## Functional Requirements

- Endpoints under the versioned base path `ApiPaths.V1` (`/api/v1`,
  docs/api-conventions.md / ADR-005). The Jira text writes `/api/shopping/...`;
  this spec applies the established `/api/v1` prefix (documented adaptation, as
  in FOR-17), i.e. `/api/v1/shopping/products`.
- `GET /api/v1/shopping/products` — list products.
- `POST /api/v1/shopping/products` — create a product.
- `PUT /api/v1/shopping/products/{id}` — update a product.
- A product can link to a `FoodItem` (optional `linkedFoodItemId`).
- Validation failures return `VALIDATION_ERROR` (400); unknown id on update
  returns `NOT_FOUND` (404) — reuse the existing FOR-88/FOR-27 `ApiError` +
  `GlobalExceptionHandler` baseline.
- Request/response DTOs are distinct from the FOR-35 domain type and the
  persistence row (ADR-005).

## Non-Functional Requirements

- Persistence: add a Flyway migration after the latest in the repo (verify the
  next free `V<N>__…`; the latest is `V3__training_session_status.sql`). Use
  `NUMERIC`/`DECIMAL` for prices (no floating-point money). Additive only
  (ADR-003).
- Prices are editable estimates; **no external price sync** (MVP).
- Integration test against the H2 PostgreSQL-mode test database (FOR-16 pattern).

## Data Model Notes

Persists `ShoppingProduct` (FOR-35). `linkedFoodItemId` is a soft reference to
the FOR-30 catalog (may be validated on create/update or left soft — see Open
Questions). `id` is generated (UUID) as in FOR-16, since the domain type carries
no identity.

## Edge Cases

- Create with missing required fields → `VALIDATION_ERROR` (per-field details).
- Update a non-existent product id → `NOT_FOUND` (404).
- Link to a non-existent food id → decide reject vs. allow-soft; document.
- Negative price/package size → validation error.

## Open Questions

- **Food-link validation**: reject `linkedFoodItemId` not in the FOR-30 catalog
  vs. store it softly. Recommend soft (nutrition foods are in-code and may
  evolve), documented; the consumer resolves it best-effort.
- Whether `DELETE` is in scope — Jira lists only GET/POST/PUT; do **not** add
  delete unless a later story asks.
