# FOR-111: ShoppingPage category filter tabs + id-based product edit

Jira: https://dbhlab.atlassian.net/browse/FOR-111
Epic: FOR-47 UI & UX

## Summary

`ShoppingPage.tsx`'s own doc comment documents two gaps as "not backed by
the API today": category grouping/filters (all items render under a single
"Todas" tab) and product-edit resolution by name instead of id (`FOR-39`'s
list item had no `productId`, so `ProductEditModal` matches
`ShoppingItem.productName` against the FOR-36 products list). FOR-106 (Done)
already threads both `productId` and `category` onto
`GET /api/v1/shopping/list`. This story is the frontend catch-up: render
real category filter tabs and resolve `ProductEditModal` by `productId`.

## User/System Flow

1. User opens Lista de compra (`/lista-compra`).
2. Items are grouped by their resolved `category` (FOR-106); tabs let the
   user filter to one category or see "Todas".
3. User clicks the edit icon on an item; `ProductEditModal` looks the
   product up by `productId` (FOR-106) instead of matching `productName`
   against the full product list.

## Functional Requirements

- Read `category` and `productId` from `GET /api/v1/shopping/list`
  (`ShoppingItem` type in `frontend/src/api/shopping.ts`) — both already
  present per FOR-106; no new API call needed.
- **Category tabs**: build the tab set from the distinct categories present
  in the current list (plus "Todas"); selecting a tab filters the rendered
  item list. Replace the current hardcoded single "Todas" tab
  (`ShoppingPage.tsx` lines ~181–185).
- **Id-based product edit**: change `ProductEditModal`'s product lookup
  (`ShoppingPage.tsx` lines ~263–285) from `products.find((p) => p.name ===
  item.productName)` to matching on `item.productId`. Keep the existing
  `not-found`/`error`/`loading` inline states as-is (FOR-113 owns migrating
  those to shared components).
- An item whose product no longer resolves by id still falls back to the
  existing "not-found" state — no crash, no silent no-op.
- No pricing/category math in the UI — categories are rendered exactly as
  returned (ADR-006, architecture-overview.md).

## Non-Functional Requirements

- Token-driven styling consistent with the existing tabs markup
  (`styles.tabs`, `styles.tab`) — this story restyles behavior, not the
  visual system.
- Accessible tabs: `role="tablist"`/`role="tab"`/`aria-selected` pattern
  already scaffolded in the current markup — extend it, don't replace the
  pattern (FOR-61).

## Data Model Notes

`ShoppingItem` (`frontend/src/api/shopping.ts`) needs `productId` and
`category` fields added to its TypeScript type if not already present,
mirroring `ShoppingListResponse.Item` (FOR-106 `api.md`). No backend change
in this story — purely a frontend consumption catch-up.

## Edge Cases

- All items share one category → tabs still render (at least "Todas" +
  that one category), no broken single-tab UI.
- Item with category `OTROS`/null (FOR-106 fallback) → grouped under a
  clearly-labelled "Otros" tab, not hidden.
- `productId` present but no longer resolves to a product (deleted product)
  → existing not-found state, not a crash.

## Open Questions

- Whether "Todas" is always the first tab or whether category tabs sort
  alphabetically — recommend "Todas" first, then categories in the FOR-106
  enum's declared order; document the final choice during implementation.
