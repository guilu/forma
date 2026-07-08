# Shopping list API

Endpoints backing the weekly shopping checklist (FOR-39). Follows the shared
[API conventions](../api-conventions.md) (ADR-005). Lives under `/api/v1`. The
list and its checked state are persisted; the budget (FOR-38) is computed on
demand from current product prices.

## `GET /api/v1/shopping/list`

Returns the current active weekly list: its items (with resolved product names,
quantity, estimated cost and checked state) plus the weekly total and monthly
estimate. `404` if there is no active list.

`200 OK`

```json
{
  "weekStartDate": "2026-07-06",
  "status": "ACTIVE",
  "items": [
    {
      "id": "…",
      "productName": "Avena 1 kg",
      "quantity": 2,
      "estimatedCostEur": 3.90,
      "checked": false
    }
  ],
  "budget": { "weeklyEur": 24.60, "monthlyEur": 106.52 }
}
```

## `PATCH /api/v1/shopping/list/items/{id}/checked`

Sets an item's checked state.

Request:

```json
{ "checked": true }
```

`200 OK`:

```json
{ "id": "…", "checked": true }
```

## Errors

Standard [`ApiError`](../api-conventions.md#standard-error-response) shape:

- 400 Bad Request — `VALIDATION_ERROR`: missing `checked`.
- 404 Not Found — `NOT_FOUND`: no active list, or unknown item id.
- 500 — `INTERNAL_ERROR`: unexpected failures only.

## Authorization

None enforced yet — single-user MVP (ADR-002).
