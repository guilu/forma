# FOR-36 Test Plan

## Scope

Verify the shopping product CRUD API and its persistence: list, create, update,
validation, not-found, and the optional food link.

## Domain Tests

N/A — the domain type is covered by FOR-35.

## Application Tests

- The service creates, updates and lists products via the repository port.
- Updating a non-existent product surfaces a not-found outcome.

## API Tests

- `GET /api/v1/shopping/products` returns the products.
- `POST` creates a product and returns it (with generated id).
- `PUT /{id}` updates a product and returns the new values.
- A product can be created/updated with a `linkedFoodItemId`.
- Missing required field → `VALIDATION_ERROR` (400) with per-field details.
- Unknown id on `PUT` → `NOT_FOUND` (404).

## UI Tests

N/A — the UI is FOR-39.

## Edge Cases

- Negative price / package size → validation error.
- Link to a non-existent food id (per the documented soft-vs-reject rule).
- Money precision preserved across save/read (NUMERIC round-trip).

## Fixtures

- A valid create payload (linked and unlinked variants).
- An invalid payload missing a required field.
- Pre-seeded products (via the repository) to assert `GET` and `PUT`.
