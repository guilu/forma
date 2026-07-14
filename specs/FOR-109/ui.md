# FOR-109 UI Spec

No UI — backend/read-model story.

## API surface

- `POST /api/v1/shopping/list/regenerate` (naming indicative) — rebuilds the
  current weekly list, updates `generatedAt`.
- `PATCH /api/v1/shopping/list/items/{itemId}` (naming indicative) — updates
  an item's quantity, recalculates `estimatedCostEur`.
- `GET /api/v1/shopping/list` (FOR-39, enriched by FOR-108) — items gain a
  `productUrl` resolved from `ShoppingProduct.url`.

FOR-118 consumes these endpoints for regenerate/quantity-edit/link-out
controls; it owns its own `ui.md`.
