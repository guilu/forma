# FOR-118 UI Spec

## Screens

- Lista de compra (`frontend/src/pages/ShoppingPage.tsx`) at
  `/lista-compra`. Mockup: `docs/5-lista-compra.png` — "Generar nueva
  lista" action and per-item link-out icons.

## Components

- Regenerate `Button` in the page header, opening a `ConfirmDialog`
  (FOR-63) before calling the command.
- Quantity +/- controls added to each item row, alongside the existing
  checkbox and edit-product icon.
- Link-out `Icon`/link per item row, reusing the existing icon-button
  pattern already used for the edit-product entry point.

## States

- Loading / Empty / Error: unchanged at the page level.
- Regenerate: idle → confirm dialog open → in-flight (button
  loading/disabled) → success (toast + refreshed list) or error (inline
  message, list unchanged).
- Quantity edit: idle → in-flight (controls disabled for that row,
  mirroring the existing `pendingId` checkbox-disable pattern) → success
  (updated quantity/cost) or error (reverted value + message).

## Interactions

- Regenerate → confirm → command → success toast / error message.
- +/- quantity → command → updated quantity + cost, or revert + error.
- Link-out icon → opens `productUrl` in a new tab (`target="_blank"
  rel="noopener noreferrer"`).

## Accessibility

- Regenerate button and confirm dialog follow FOR-63's keyboard-operable,
  focus-trapped destructive-confirmation pattern.
- Quantity +/- buttons have accessible names identifying the item (e.g.
  `aria-label="Aumentar cantidad de {productName}"`).
- Link-out control is a real `<a>` with a discernible accessible name and
  an indication that it opens in a new tab.

## Responsive Behavior

- Quantity controls and link-out icon fit within the existing item-row
  layout on mobile without introducing horizontal scroll; regenerate
  button remains reachable above the fold or via the existing page header
  pattern.
