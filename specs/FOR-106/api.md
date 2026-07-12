# FOR-106 API Spec

## Endpoints

Extends existing shopping endpoints (no new routes):

### GET /api/v1/shopping/list (FOR-39)

Each item now carries `productId` and `category`.

### POST/PUT/GET /api/v1/shopping/products (FOR-36)

The product create/update/read now include `category`.

## Request

FOR-36 product create/update body gains `category` (optional; default when
omitted):

```json
{
  "name": "Plátano",
  "estimatedPriceEur": 2.03,
  "category": "FRUTAS_Y_VERDURAS"
}
```

## Response

`GET /api/v1/shopping/list` (excerpt — new fields marked, existing fields
unchanged):

```json
{
  "weekStartDate": "2026-06-08",
  "status": "ACTIVE",
  "items": [
    {
      "id": "…",
      "productId": "…",
      "productName": "Plátano",
      "category": "FRUTAS_Y_VERDURAS",
      "quantity": 14,
      "estimatedCostEur": 2.03,
      "checked": false
    }
  ],
  "budget": { "weeklyEur": 42.35, "monthlyEur": 183.85 }
}
```

- `productId`: the FOR-36 product id (already on the domain item; now surfaced) —
  lets the UI resolve product edits by id instead of by name.
- `category`: resolved from the product. Suggested enum (document the final set):
  `FRUTAS_Y_VERDURAS`, `PROTEINAS`, `LACTEOS_Y_HUEVOS`, `CEREALES_Y_LEGUMBRES`,
  `GRASAS_Y_ACEITES`, `OTROS`. Default `OTROS` when the product has none.

## Errors

Standard [`ApiError`](../../docs/api-conventions.md) shape:

- 400 Bad Request — `VALIDATION_ERROR`: invalid `category` value on product write.
- 404 Not Found — `NOT_FOUND`: unchanged for unknown product/list.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).

## Validation

`category` (if a closed enum) must be one of the documented values on product
write; optional, defaulting to `OTROS`. Existing validations unchanged.
