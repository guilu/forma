# FOR-36 API Spec

Follows the shared conventions in `docs/api-conventions.md` (ADR-005) and the
FOR-88/FOR-27 `ApiError` baseline. Mounted under `ApiPaths.V1` (`/api/v1`).

## Endpoints

### GET /api/v1/shopping/products

Lists shopping products.

### POST /api/v1/shopping/products

Creates a product. Returns `201 Created` with the product (including generated
`id`).

### PUT /api/v1/shopping/products/{id}

Updates a product. Returns `200 OK` with the updated product; `404` if the id
does not exist.

## Request

`POST` / `PUT` body:

```json
{
  "name": "Avena 1 kg",
  "url": "https://tienda.example/avena",
  "packageSize": "1 kg",
  "estimatedPriceEur": 1.95,
  "linkedFoodItemId": "oats",
  "notes": "Marca blanca"
}
```

`name` and `estimatedPriceEur` are required; `linkedFoodItemId`, `url`,
`packageSize`, `notes` are optional. `pricePerUnitEur` is derived/stored per the
FOR-35 rule.

## Response

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

## Errors

Standard `ApiError` shape:

- 400 Bad Request — `VALIDATION_ERROR`: missing/invalid field (per-field
  `details`).
- 404 Not Found — `NOT_FOUND`: unknown product id on `PUT`.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002). No `DELETE` in this story.

## Validation

- `name`: required, non-blank.
- `estimatedPriceEur`: required, positive (`NUMERIC`).
- `packageSize`: optional; if numeric, used to derive `pricePerUnitEur`.
- `linkedFoodItemId`: optional; soft reference to a FOR-30 food (see spec Open
  Questions).
- Bean Validation on the request DTO → `VALIDATION_ERROR` via the existing
  `GlobalExceptionHandler`; no hand-rolled validation path.
