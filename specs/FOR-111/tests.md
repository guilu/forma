# FOR-111 Test Plan

## Scope

Verify category tabs correctly filter the shopping list and that product
edit resolves by `productId` instead of `productName`.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the existing FOR-106 contract; no backend change.

## UI Tests

- Category tabs render one tab per distinct category present in the list,
  plus "Todas".
- Selecting a category tab filters the rendered items to that category;
  `aria-selected` updates accordingly.
- Selecting "Todas" shows all items regardless of category.
- Clicking an item's edit icon opens `ProductEditModal` with the product
  matched by `productId`, not by `productName`.
- Two products with the same name but different ids: editing either item
  opens the correct distinct product (regression guard for the old
  name-matching bug).
- An item whose `productId` no longer resolves to a product → existing
  not-found state renders, no crash.

## Edge Cases

- All items in a single category → tabs still render correctly (no broken
  single-tab layout).
- Item with `category: OTROS`/null → grouped under "Otros", not dropped
  from any tab.
- Empty category (a tab with zero items after filtering, e.g. stale
  selection after a list refresh) → scoped empty state, not a crash.

## Fixtures

- A shopping list fixture with items spanning at least three categories,
  including one `OTROS` item and two same-name/different-id products.
