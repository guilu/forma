# Shopping products API

CRUD endpoints for the editable shopping product catalog (FOR-36). Follows the
shared [API conventions](../api-conventions.md) (ADR-005) and the `ApiError`
baseline. Lives under `/api/v1`. Prices are editable estimates (EUR); there is no
external price sync.

## `GET /api/v1/shopping/products`

Lists products (ordered by name).

## `POST /api/v1/shopping/products`

Creates a product. `201 Created` with the product (generated `id`).

## `PUT /api/v1/shopping/products/{id}`

Updates a product. `200 OK` with the updated product; `404` if the id is unknown.

Request (`POST` / `PUT`):

```json
{
  "name": "Avena 1 kg",
  "url": "https://tienda.example/avena",
  "packageSize": "1 kg",
  "estimatedPriceEur": 1.95,
  "pricePerUnitEur": 1.95,
  "linkedFoodItemId": "oats",
  "notes": "Marca blanca"
}
```

`name` and `estimatedPriceEur` are required; the rest are optional. `lastCheckedAt`
is **not** client-supplied — the server stamps it when the product is saved.

Response:

```json
{
  "id": "…",
  "name": "Avena 1 kg",
  "url": "https://tienda.example/avena",
  "packageSize": "1 kg",
  "estimatedPriceEur": 1.95,
  "pricePerUnitEur": 1.95,
  "linkedFoodItemId": "oats",
  "lastCheckedAt": "2026-07-08T10:00:00Z",
  "notes": "Marca blanca"
}
```

- `linkedFoodItemId`: optional soft link to a nutrition food (FOR-30). Null
  fields are omitted.

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 400 Bad Request — `VALIDATION_ERROR`: missing/invalid field (per-field
  `details`); e.g. blank `name` or non-positive `estimatedPriceEur`.
- 404 Not Found — `NOT_FOUND`: unknown product id on `PUT`.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002). No `DELETE` in this version.
