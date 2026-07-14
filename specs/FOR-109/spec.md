# FOR-109: [STUB] Shopping list actions backend (regenerate, quantity edit, product link-out)

Jira: https://dbhlab.atlassian.net/browse/FOR-109
Epic: FOR-96

## Summary

Add the command side that FOR-39's checklist is currently missing:
regenerate the current weekly shopping list, edit an item's quantity with a
recalculated line cost, and expose a provider product link/add-to-cart
reference so the UI can link out. Today `ShoppingListService` only exposes
`currentView()` (read) and `setChecked()` (toggle); there is no regenerate
command and no quantity-edit command. Unblocks FOR-118 (interactive actions
UI).

## User/System Flow

1. **Regenerate**: client calls a new command endpoint; backend rebuilds the
   current weekly list (same generation logic FOR-37 already uses to create
   a list, re-run on demand) and updates `generatedAt` (FOR-108).
2. **Quantity edit**: client calls a new command endpoint with an item id
   and a new quantity; backend validates (`quantity >= 1`, mirroring
   `ShoppingListItem`'s existing invariant), recalculates
   `estimatedCostEur` from the product's stored price, and persists.
3. **Product link-out**: client reads the enriched list (FOR-108) and gets a
   `productUrl` (or similarly named) field per item, resolved from
   `ShoppingProduct.url` — already a domain field — so a "ver en tienda" /
   add-to-cart link can be rendered without a new product lookup call.

## Functional Requirements

- **Regenerate command**: new endpoint (e.g. `POST /api/v1/shopping/list/
  regenerate`) that re-runs the FOR-37 list-generation logic for the
  current week and replaces items; updates `generatedAt` (FOR-108).
  Existing `checked` state is expected to reset on regeneration (a new list
  is a new checklist) — document if the implementation instead tries to
  preserve it.
- **Quantity-edit command**: new endpoint (e.g. `PATCH /api/v1/shopping/
  list/items/{itemId}`) accepting a new quantity; recalculates
  `estimatedCostEur` from the product's `estimatedPriceEur`/
  `pricePerUnitEur` (mirrors the read-side cost fields already on
  `ShoppingListItem`); rejects `quantity < 1` (400).
- **Product link-out**: resolve `ShoppingProduct.url` (already a domain
  field — see Data Model Notes) per list item and surface it on the FOR-108
  enriched item response as a `productUrl`. Provider is Mercadona for the
  MVP per the domain's original naming (`docs/domain-model.md` calls it
  "MercadonaProduct"; the type was generalized to `ShoppingProduct` to stay
  provider-agnostic) — this story does not build a Mercadona-specific
  add-to-cart integration, only exposes the stored product URL as a
  link-out target.
- Thin controllers; commands validate at the boundary and call into
  `ShoppingListService`/`ShoppingProductService` (ADR-001, ADR-005).

## Non-Functional Requirements

- Single-user MVP (ADR-002) — no cross-user authorization added here.
- Consistent `ApiError` shape for validation failures (ADR-005).
- No client-side price math — cost recalculation stays server-side (mirrors
  the existing `ShoppingBudgetService` boundary).

## Data Model Notes

`ShoppingListService` (`backend/src/main/java/dev/diegobarrioh/forma/
application/ShoppingListService.java`) currently exposes only `currentView()`
and `setChecked(itemId, checked)` — verified directly against the source; no
regenerate or quantity-edit method exists. `ShoppingProduct.url` already
exists as an optional field (`backend/.../domain/ShoppingProduct.java`) —
the "product link" requirement is largely a wiring/surfacing task, not new
domain modeling, once FOR-108's `productUrl`-style field is added to the
enriched item response. Quantity/cost recalculation should reuse
`ShoppingProduct.estimatedPriceEur`/`pricePerUnitEur`, the same fields the
existing generation logic already uses.

## Edge Cases

- Regenerate called with no products available → an empty (but valid) list,
  not an error (mirrors `ShoppingList`'s "empty list is valid" invariant).
- Quantity edit on an item whose product id no longer resolves → reject
  with a clear error rather than computing a cost from nothing.
- Quantity edit to the same value → idempotent, no error.
- Product with no `url` → `productUrl: null` in the response, not a broken
  link.

## Open Questions

- Whether regeneration preserves `checked` state for items that reappear
  unchanged, or always resets — recommend always resetting for MVP
  simplicity; document the final decision.
- Exact route/verb naming for the two new commands — align with existing
  FOR-39 route conventions during implementation.
