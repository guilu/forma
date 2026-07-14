# FOR-108 Test Plan

## Scope

Verify `unit`, `servings` and `generatedAt` are threaded correctly through
the shopping read model without changing existing list/budget/check
behavior.

## Domain Tests

- `ShoppingListItem` (or its extended form) accepts a unit value and an
  optional servings value.
- `ShoppingList` (or its extended form) carries a `generatedAt` value.

## Application Tests

- `ShoppingListView.Entry` carries `unit` and `servings` resolved from the
  domain item.
- `ShoppingListView` carries `generatedAt` resolved from the domain list.
- Existing budget computation (`ShoppingBudgetService`) is unaffected by the
  new fields.

## API Tests

- `GET /api/v1/shopping/list` response includes `unit` and `servings` per
  item, and `generatedAt` at the list level.
- Item with no linked food product → `servings: null` in the response, not
  omitted or fabricated.
- Pre-existing (pre-migration) list/item data → backfilled defaults appear
  in the response instead of nulls that would break the FOR-106 shape.
- Existing fields (`productId`, `category`, `quantity`, `estimatedCostEur`,
  `checked`, `budget`) unchanged — regression check against FOR-106/FOR-39
  contract.

## UI Tests

N/A — backend story.

## Edge Cases

- Unit value missing on a legacy row → default, not a validation error.
- `generatedAt` missing on a legacy list → backfilled, not absent from the
  payload.

## Fixtures

- A shopping list with items with and without a linked food product
  (servings vs. no servings).
- A pre-migration list row to exercise the backfill path.
