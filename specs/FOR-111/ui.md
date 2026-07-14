# FOR-111 UI Spec

## Screens

- Lista de compra (`frontend/src/pages/ShoppingPage.tsx`) at `/lista-compra`.
  Mockup: `docs/5-lista-compra.png`.

## Components

- Category tabs (`role="tablist"`) — extends the existing scaffolded markup
  in `ShoppingPage.tsx` to render one tab per distinct `category` present in
  the list, plus "Todas".
- `ProductEditModal` (nested in `ShoppingPage.tsx`) — lookup changes from
  name-matching to `productId`-matching; visual markup unchanged.

## States

- Loading: unchanged (`LoadingState`, page-level).
- Empty: unchanged (`EmptyState` when the list has zero items); a selected
  category tab with zero matching items shows the same empty pattern scoped
  to that tab.
- Error: unchanged (`ErrorState` with retry, page-level).
- Success: items grouped/filterable by category; edit resolves by id.

## Interactions

- Click a category tab → filters the visible item list to that category;
  `aria-selected` moves to the clicked tab.
- Click the edit icon on an item → opens `ProductEditModal`, now resolved by
  `productId`.

## Accessibility

- Tabs keyboard-operable (arrow-key or tab-order navigation consistent with
  the existing `role="tablist"`/`role="tab"` scaffold), visible focus state
  (FOR-61).
- Tab labels include the item count per category (existing "Todas (N)"
  pattern), read by screen readers.

## Responsive Behavior

- Tabs wrap or scroll horizontally on narrow viewports rather than
  overflowing; matches the existing `styles.tabs` mobile behavior.
