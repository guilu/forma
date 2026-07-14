# FOR-109 Test Plan

## Scope

Verify the regenerate command, the quantity-edit command with cost
recalculation, and the product link-out field, without regressing the
existing checklist/budget behavior.

## Domain Tests

- `ShoppingListItem` quantity update rejects `< 1` (existing invariant,
  exercised through the new command path).

## Application Tests

- Regenerate use case rebuilds the list and updates `generatedAt`.
- Regenerate on an empty product catalog produces a valid empty list, not an
  error.
- Quantity-edit use case recalculates `estimatedCostEur` from the product's
  stored price.
- Quantity-edit on an unresolvable product id is rejected with a clear
  error, not a fabricated cost.

## API Tests

- `POST .../regenerate` → 200, subsequent `GET` reflects a new
  `generatedAt` and rebuilt items.
- `PATCH .../items/{itemId}` with a valid new quantity → 200, subsequent
  `GET` reflects the new quantity and recalculated cost.
- `PATCH .../items/{itemId}` with `quantity < 1` → 400
  `VALIDATION_ERROR`.
- `GET /api/v1/shopping/list` → each item's `productUrl` matches the
  linked product's stored `url`, or `null` when the product has none.
- Existing `setChecked`/budget endpoints unaffected (regression check).

## UI Tests

N/A — backend story.

## Edge Cases

- Regenerate when a previously-checked item disappears from the new list →
  no error, item simply absent.
- Quantity edit to the item's current value → idempotent 200, no side
  effects.

## Fixtures

- A seeded weekly list with checked and unchecked items for the regenerate
  test.
- Products with and without a stored `url` for the link-out test.
