# FOR-106 Test Plan

## Scope

Verify `productId` + `category` appear on shopping list items, `category` is added
to the product model end-to-end, and existing behavior is preserved.

## Domain Tests

- `ShoppingProduct` accepts a `category`; documented default/`OTROS` when absent.
- Adding the field keeps existing validation intact.

## Application Tests

- The shopping list view resolves each item's `category` from its product and
  carries `productId` through.

## API Tests

- `GET /api/v1/shopping/list` items include `productId` and `category`.
- FOR-36 product create/update/read round-trips `category`.
- Existing list/budget/check responses otherwise unchanged (backward compatible).
- Product with no category → default; item still returned.

## UI Tests

N/A — backend story (FOR-55 consumes it).

## Edge Cases

- Old product rows without a category load with the default.
- List item whose product id doesn't resolve → category default/null, no crash.

## Fixtures

- Products across a few categories; a list referencing them; a product with no
  category.
