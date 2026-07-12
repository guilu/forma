# FOR-106 AI Context

## Story

FOR-106 — Expose product id and category on shopping list items
(https://dbhlab.atlassian.net/browse/FOR-106)

## Intent

Give the shopping UI real category grouping/filtering and id-based product edit
resolution. Success is `productId` + `category` on each list item read model,
backed by a `category` added to the product model.

## Relevant Documents

- `AGENTS.md`
- `docs/api-conventions.md`, `docs/adr/ADR-005-api-design.md`,
  `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-35/`/`FOR-36/` (product model/CRUD), `specs/FOR-37/`..`FOR-39/`
  (list/budget/checklist)
- Jira: https://dbhlab.atlassian.net/browse/FOR-106

## Domain Notes

- `domain/ShoppingListItem` ALREADY has `productId` — the view/DTO just drop it.
  Threading it through `ShoppingListView.Entry` + `ShoppingListResponse.Item` is
  additive and low-risk.
- `domain/ShoppingProduct` has NO `category`. Add it (domain + FOR-16/FOR-36
  persistence + migration + product API). Resolve item category via the product
  (item.productId → product.category) where the view is assembled (same place
  product names are resolved).
- GOTCHAS: FOR-39 UUID param binding differs H2 vs PostgreSQL; FOR-100 found H2
  rejects multi-column `ALTER TABLE ADD COLUMN a, b` (use one statement per
  column). Adding record components to `ShoppingProduct` will break every `new
  ShoppingProduct(...)` call site (main + tests + seed) — update them all.

## Architectural Constraints

- Domain framework-free (ADR-001). DTOs distinct from domain (ADR-005). Migration
  additive/backward compatible. No category fabrication in the UI.

## Common Pitfalls

- Only exposing `productId` and forgetting the `category` model work (or vice
  versa).
- Breaking `new ShoppingProduct(...)` call sites when adding the field.
- Fabricating categories client-side instead of sourcing from the product.

## Suggested Implementation Order

1. Thread `productId` through `ShoppingListView.Entry` + `ShoppingListResponse.Item` (+ tests).
2. Add `category` to `ShoppingProduct` (+ validation) and update all call sites.
3. Persistence migration + JDBC read/write; FOR-36 product API (create/update/read).
4. Resolve + expose `category` on the list item; @WebMvcTest + round-trip tests.

## Validation

Run `./gradlew test spotlessApply` from `backend/`; run migrations against the
local DB. Confirm list items carry `productId` + `category` and product CRUD
round-trips category.
