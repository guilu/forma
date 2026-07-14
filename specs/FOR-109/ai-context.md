# FOR-109 AI Context

## Story

FOR-109 — [STUB] Shopping list actions backend (regenerate, quantity edit,
product link-out) (https://dbhlab.atlassian.net/browse/FOR-109)

## Intent

FOR-39's shopping checklist is currently read-only plus a single toggle
(`setChecked`). This story adds the commands FOR-118's interactive UI needs:
regenerate the week's list, edit an item's quantity (with recalculated
cost), and surface a product link-out so the user can jump to the provider
page. Keeps the domain provider-agnostic even though the MVP's practical
provider is Mercadona.

## Blocked by

None. This story blocks: FOR-118.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-001-architecture.md` (framework-free domain, thin
  controllers)
- `docs/adr/ADR-002-authentication.md` (single-user MVP)
- `docs/adr/ADR-005-api-design.md` (commands vs. queries, `ApiError` shape)
- `specs/FOR-108/spec.md` — the read-model enrichment (`generatedAt`,
  `unit`, `servings`) this story's commands interact with
- Jira: https://dbhlab.atlassian.net/browse/FOR-109

## Domain Notes

- `backend/src/main/java/dev/diegobarrioh/forma/application/
  ShoppingListService.java` — currently only `currentView()` and
  `setChecked(itemId, checked)`; no regenerate or quantity-edit method.
- `backend/.../domain/ShoppingProduct.java` — already has a `url` field
  (optional); the "product link-out" requirement is mostly wiring, not new
  domain modeling.
- `backend/.../domain/ShoppingListItem.java` — quantity invariant
  (`>= 1`) already exists; reuse it rather than re-validating ad hoc.

## Architectural Constraints

- Commands go through `ShoppingListService`/`ShoppingProductService`, not
  directly from controllers (ADR-001).
- Cost recalculation is server-side only — no client-side price math
  (mirrors `ShoppingBudgetService`'s existing boundary).
- DTOs distinct from domain; thin controllers (ADR-005).

## Common Pitfalls

- Recomputing cost with stale product price data instead of the current
  stored `estimatedPriceEur`/`pricePerUnitEur`.
- Silently ignoring an unresolvable product id on quantity edit instead of
  rejecting with a clear error.
- Building a Mercadona-specific add-to-cart integration when the story only
  asks for a link-out reference (`url`) — do not over-scope.

## Suggested Implementation Order

1. Regenerate use case + endpoint; updates `generatedAt` (depends on
   FOR-108's field existing).
2. Quantity-edit use case (validation + cost recalculation) + endpoint.
3. Surface `productUrl` on the FOR-108 enriched item response.
4. Tests per `tests.md`; confirm FOR-38/FOR-39 behavior unchanged.

## Validation

Backend build + tests (AGENTS.md Verification guidance). Exercise
regenerate and quantity-edit end to end, then re-`GET` the list to confirm
persisted state.
