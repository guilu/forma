# FOR-118: Shopping list: interactive actions UI (regenerate, qty edit, link-out)

Jira: https://dbhlab.atlassian.net/browse/FOR-118
Epic: FOR-47 UI & UX

## Summary

`ShoppingPage.tsx`'s doc comment documents this gap: "'Generar nueva
lista', per-item Mercadona link-out/add-to-cart icons and +/- quantity
editing — no regenerate, link-out or item-quantity-update endpoint exists;
omitted entirely rather than shown inactive." FOR-109 (backend) adds those
three commands. This story wires the UI: a regenerate action with
confirmation and feedback, +/- quantity controls that update the line
cost, and a provider link-out/add-to-cart affordance per item.

## User/System Flow

1. User opens Lista de compra.
2. **Regenerate**: user clicks "Generar nueva lista"; a confirmation
   (FOR-63 destructive-action pattern — regenerating discards current
   checked state) precedes the call; on success, a toast confirms and the
   list re-renders with the new items and `generatedAt`.
3. **Quantity edit**: user clicks +/- controls on an item; quantity updates
   optimistically or on response, and the line's estimated cost updates to
   match the recalculated backend value.
4. **Link-out**: user clicks a per-item link-out icon; opens the
   provider's `productUrl` (FOR-109) in a new tab, or is hidden when the
   item has no URL.

## Functional Requirements

- **Regenerate control**: a button (mockup: "Generar nueva lista") that
  opens a confirm dialog (reuse `ConfirmDialog`/`Modal`, FOR-63 pattern)
  before calling the FOR-109 regenerate endpoint; success feedback via
  `useNotify().success()`; failure surfaces a clear error, list state
  unchanged.
- **Quantity +/- controls**: per item, increment/decrement buttons calling
  the FOR-109 quantity-edit endpoint; disable during the in-flight request
  (mirrors the existing `pendingId`/checkbox-disable pattern already used
  for `toggle`); on success, both quantity and `estimatedCostEur` update
  from the response (never computed client-side).
- **Provider link-out**: render `productUrl` (FOR-109/FOR-108) as an
  external link/icon per item when present; omitted (not disabled) when
  the item has no URL, following the existing "omit, don't show inactive"
  precedent already used elsewhere on this page.
- Reuse `useNotify()` for success/error feedback (FOR-63), matching the
  existing `toggle()` handler's pattern in `ShoppingPage.tsx`.

## Non-Functional Requirements

- Destructive-confirmation pattern for regenerate (FOR-63) — explicit
  confirm, cancel has no side effect.
- Accessible: all new controls keyboard-operable with visible focus and
  labelled (FOR-61); link-out opens in a new tab with an accessible
  "opens in new window" indication.
- No client-side cost/quantity math — every displayed value after an
  action comes from the backend response.

## Data Model Notes

Consumes FOR-109's three new command endpoints and FOR-108's `productUrl`/
enriched fields. No new frontend-only data model — extends
`frontend/src/api/shopping.ts` with the new command functions.

## Edge Cases

- Regenerate cancelled at the confirm step → no request sent, list
  unchanged (FOR-63 edge case: "destructive action cancelled → no side
  effect").
- Quantity decrement below 1 → button disabled at 1 (client-side guard
  matching the backend's `quantity >= 1` invariant), not a failed request.
- Quantity-edit request fails (e.g. product no longer resolves, FOR-109
  edge case) → clear error shown, quantity reverts to its last known
  value, no stuck pending state (FOR-63 edge case).
- Item with no `productUrl` → link-out control omitted for that row only,
  other rows unaffected.

## Open Questions

- Whether quantity +/- updates optimistically (instant UI change, rollback
  on failure) or waits for the response — recommend waiting for the
  response for MVP simplicity (matches the existing `toggle()` pattern,
  which also waits), revisit for optimistic updates later if latency is a
  problem.
