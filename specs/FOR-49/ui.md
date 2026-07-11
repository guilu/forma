# FOR-49 UI Spec

## Screens

- Application shell wrapping every route (`frontend/src/layout/AppShell.tsx`).
  Shared by all mockups; reference `docs/1-dashboard.png` for the chrome.

## Components

- `AppShell` — layout grid: sidebar + (topbar + content outlet).
- `Sidebar` (desktop) — brand lockup, `NAV_ITEMS` links, footer "Withings ·
  Conectado" status entry.
- `Topbar` — hamburger toggle, page title/subtitle slot, notification bell,
  account area (avatar + name, static placeholder).
- `MobileNav` — compact bottom bar of `primary` items + "Más" overflow.
- Reuse `components/Brand.tsx`, `components/Icon.tsx`.

## States

- Loading: N/A (shell is static); content area owns its own loading.
- Empty: N/A.
- Error: unknown route → `NotFoundPage` inside the shell.
- Success: shell renders with the active section highlighted.

## Interactions

- Clicking a nav item routes and updates the active highlight.
- Hamburger toggles the sidebar on smaller viewports.
- Notification bell + account area are visual placeholders (wired later by
  FOR-63 / a future auth story).

## Accessibility

- `<nav>` landmark for navigation; `aria-current="page"` on the active item.
- Header is a `<header>` landmark, main content a `<main>` landmark.
- Keyboard-operable nav links and toggle with visible focus (theme tokens).

## Responsive Behavior

- Desktop: fixed sidebar (`--sidebar-width`) + topbar (`--topbar-height`);
  content scrolls.
- Tablet: sidebar may collapse to icons or toggle open.
- Mobile: sidebar hidden; bottom `MobileNav` with primary items + "Más"; no
  horizontal scroll (docs/ui-guidelines.md mobile priorities).
