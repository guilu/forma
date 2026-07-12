# FOR-106: Expose product id and category on shopping list items

Jira: https://dbhlab.atlassian.net/browse/FOR-106
Epic: FOR-95 UI Backend Enablers

## Summary

Surface `productId` and `category` on each shopping list item in the read model
so the UI can group/filter the list by category and resolve product edits by id
(not by name). `productId` already exists on the domain item but is dropped by the
view/DTO — threading it through is trivial. `category` does **not** exist on the
product model yet, so this story adds it to `ShoppingProduct` (domain +
persistence + FOR-36 API) and resolves it per list item.

## User/System Flow

1. Client calls `GET /api/v1/shopping/list` (FOR-39).
2. Each item now carries `productId` and its product `category` (resolved from the
   FOR-36 product).
3. FOR-55 groups/filters by category and resolves product edits by id.

## Functional Requirements

- **Thread `productId`** from `domain/ShoppingListItem` (which already has it)
  through `application/ShoppingListView.Entry` and
  `delivery/shopping/ShoppingListResponse.Item` (additive).
- **Add `category`** to `ShoppingProduct` (domain field), through FOR-16-style
  persistence + a migration, and the FOR-36 product API (create/update/read).
  Choose a representation — recommend a light enum (Frutas y verduras / Proteínas
  / Lácteos y huevos / Cereales y legumbres / Grasas y aceites / Otros) or a
  validated string; document. Nullable/`OTROS` default → backward compatible.
- **Resolve `category`** per list item (item → product by `productId` → category)
  when building the view, mirroring the existing name resolution; expose it on the
  list item DTO.
- Keep existing list/budget/check behavior unchanged (FOR-38/FOR-39). DTOs
  distinct from domain (ADR-005); thin controllers (ADR-001).

## Non-Functional Requirements

- Additive/backward compatible where possible; existing rows/payloads still work.
- No UI-side category fabrication or pricing math (read models only).

## Data Model Notes

`domain/ShoppingListItem` already has `productId` (+ quantity, estimatedCostEur,
checked). `application/ShoppingListView.Entry` and
`delivery/shopping/ShoppingListResponse.Item` currently omit it. `ShoppingProduct`
(name, url, packageSize, estimatedPriceEur, pricePerUnitEur, linkedFoodItemId,
lastCheckedAt, notes) has **no category** — add one. Product persistence is FOR-36
(JDBC + migration); watch the known H2-vs-PostgreSQL UUID param-binding gotcha
(FOR-39) and the multi-column `ALTER TABLE ADD COLUMN` H2 gotcha (FOR-100 — one
column per statement).

## Edge Cases

- Product with no category (old rows) → default `OTROS`/null; the list still
  builds and the UI falls back to a single group.
- List item whose product id no longer resolves → category null/`OTROS`, no crash
  (mirror the existing name-resolution fallback).

## Open Questions

- Category as enum vs free-text string — recommend a small enum for reliable
  grouping/filtering; document the set.
- Scope split: `productId` (trivial) vs `category` (domain + migration) could be
  two stories. This story keeps both per the Jira intent; if the category model
  work grows, split during implementation and document — do not silently drop it.
