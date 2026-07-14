# FOR-108: Expose shopping item quantity, unit & servings over HTTP

Jira: https://dbhlab.atlassian.net/browse/FOR-108
Epic: FOR-95

## Summary

Read-model enrichment sibling of FOR-106 (which threaded `productId` and
`category` onto shopping list items): add a unit of measure and a servings
count per item, plus a `generatedAt` timestamp on the weekly list itself, so
FOR-117 can render a richer item line ("14 ud · 2 raciones") and show when
the list was generated. Additive, read-only — no new commands, no change to
existing budgeting or checklist behavior.

## User/System Flow

1. Client calls `GET /api/v1/shopping/list` (FOR-39), same as today.
2. Each item now additionally carries `unit` and `servings`.
3. The list payload additionally carries `generatedAt`.
4. FOR-117 renders the enriched fields; no other consumer changes.

## Functional Requirements

- **`unit`**: unit of measure for the item's `quantity` (e.g. `UD`, `G`,
  `KG`, `L`, `PAQUETE`). `domain/ShoppingListItem` currently has `quantity`
  as a bare `int` ("number of units/packages") with no unit-of-measure
  field — add one. Recommend a small enum mirroring the `ShoppingCategory`
  pattern from FOR-106, nullable/defaulted for backward compatibility.
- **`servings`**: number of servings the line item represents, when
  applicable (nutrition-linked items via `linkedFoodItemId` on
  `ShoppingProduct`); nullable when not applicable (e.g. non-food items).
- **`generatedAt`**: timestamp of when the current weekly `ShoppingList` was
  generated/created. `domain/ShoppingList` currently has no timestamp field
  at all — add one.
- Thread the new fields through `domain/ShoppingListItem` →
  `application/ShoppingListView.Entry` → `delivery/shopping/
  ShoppingListResponse.Item` (and the list-level `generatedAt` onto
  `ShoppingListResponse`), mirroring exactly how FOR-106 threaded
  `productId`/`category`.
- Keep existing list/budget/check behavior unchanged (FOR-38/FOR-39). DTOs
  remain distinct from domain (ADR-005); thin controllers (ADR-001).

## Non-Functional Requirements

- Additive/backward compatible: existing rows/payloads keep working; new
  fields default sensibly (`unit` defaults to a generic "unit" value,
  `servings` defaults to `null`, `generatedAt` backfills to a migration-time
  value for pre-existing lists).
- No UI-side unit conversion or serving-size math — this story only exposes
  stored/derived values from the backend (read models only, per
  architecture-overview.md).

## Data Model Notes

`domain/ShoppingListItem` (record: `productId`, `quantity`,
`estimatedCostEur`, `checked`) has no `unit` or `servings` field today —
verified directly against the source. `domain/ShoppingList` (record:
`weekStartDate`, `status`, `items`, `notes`) has no timestamp field today —
verified directly against the source. Persistence changes go through
FOR-37/FOR-39's existing shopping-list persistence adapter plus a migration
(ADR-003); watch the same H2-vs-PostgreSQL multi-column `ALTER TABLE ADD
COLUMN` gotcha noted in FOR-100/FOR-106 (one column per statement).

## Edge Cases

- Pre-existing item with no unit recorded → default unit value, not null
  breaking the response shape.
- Pre-existing list with no `generatedAt` recorded (created before this
  migration) → backfilled to a documented sentinel (e.g. migration
  timestamp or `weekStartDate`), never left absent from the payload.
- Item not linked to a food product (`linkedFoodItemId` absent) →
  `servings: null`, not a fabricated value.

## Open Questions

- Unit as a closed enum vs. free-text string — recommend a small enum for
  consistency with the FOR-106 `ShoppingCategory` pattern; document the
  final set during implementation.
- Whether `generatedAt` is set once at list creation or updated on
  regeneration — this story only exposes the field; FOR-109's regenerate
  command is expected to be the field's primary writer.
