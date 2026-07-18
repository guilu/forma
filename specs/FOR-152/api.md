# FOR-152 API Spec

> Reuses the existing food, shopping-products and shopping-list/budget read models. Adds a cost
> threshold signal to the budget payload. Confirm paths against `ApiPaths.java` (`/api/v1`) and the
> nutrition/shopping controllers. Aligns with ADR-005.

## Endpoints

### GET /api/v1/nutrition/foods (existing — confirm path)

Now returns Diego's 23 foods (Macros sheet) instead of the 12 demo foods. Shape unchanged.

### GET /api/v1/shopping/products (existing — confirm path)

Now returns the 23 Mercadona products with real `url`, `price`, `category`, `packageSize`, quantity.

### GET /api/v1/shopping/list (existing — confirm path)

Weekly shopping list + budget. Budget gains a threshold signal.

## Response (budget with threshold)

```json
{
  "items": [ /* … 23-product-derived items … */ ],
  "budget": {
    "weeklyEur": 104.11,
    "monthlyEur": 450.79,
    "weeklyThresholdEur": 120.00,
    "overThreshold": false
  }
}
```
- `weeklyEur`/`monthlyEur` stay backend-derived (never recomputed client-side).
- `weeklyThresholdEur` + `overThreshold` are the new fields FOR-150 rule 6 and the dashboard consume.
- Field names to match the existing budget DTO conventions.

## Errors

- No new client input. Standard errors on persistence failure. Empty list → existing empty behavior.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with existing shopping/nutrition endpoints.

## Validation

- Product prices strictly positive (existing `ShoppingProduct` rule); threshold non-negative.
- No request body for the reads.
