# FOR-118 Test Plan

## Scope

Verify the regenerate flow (with confirmation and feedback), quantity +/-
controls (with cost recalculation and error revert), and the link-out
control.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-109 command endpoints; no backend change in this
story.

## UI Tests

- Clicking "Generar nueva lista" opens a confirm dialog; cancelling sends
  no request and leaves the list unchanged.
- Confirming regenerate calls the command, shows a success toast on
  success, and refreshes the rendered list/`generatedAt`.
- Regenerate failure shows a clear error message; list state unchanged.
- Clicking "+" on an item calls the quantity-edit command and updates both
  quantity and estimated cost from the response.
- Clicking "-" at quantity 1 is disabled (no request sent).
- A failing quantity-edit request reverts the displayed quantity and shows
  an error message, without leaving the row stuck in a pending state.
- An item with `productUrl` renders a link-out control opening that URL in
  a new tab; an item without `productUrl` renders no link-out control.

## Edge Cases

- Rapid repeated +/- clicks on the same item → requests serialize/disable
  during flight (mirrors existing `pendingId` guard), no duplicate/racing
  updates.
- Regenerate while a quantity edit is in flight → documented behavior
  (e.g. disable regenerate during any in-flight item action, or vice
  versa) to avoid a race; test whichever is chosen.

## Fixtures

- A shopping list fixture with items that do and don't have a
  `productUrl`, and at least one item at `quantity: 1` for the decrement-
  disabled test.
