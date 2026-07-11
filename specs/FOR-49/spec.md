# FOR-49: Create responsive application shell

Jira: https://dbhlab.atlassian.net/browse/FOR-49
Epic: FOR-47 UI & UX

## Summary

Provide the responsive application shell that every MVP module renders inside:
layout container, header (topbar), primary navigation (sidebar), mobile
navigation, content area and a user/account area. Frontend-only. FOR-81 already
bootstrapped an `AppShell` + `Sidebar`/`Topbar`/`MobileNav` + centralized
`NAV_ITEMS`; this story hardens that shell to the mockups (all 8 screens share
the same chrome) and confirms it carries every MVP section.

## User/System Flow

1. The app renders `AppShell` (layout route in `app/routes.tsx`).
2. Sidebar (desktop) / bottom nav (mobile) derive from `NAV_ITEMS`; the topbar
   shows the FORMA brand, a menu toggle, notifications entry and the account
   area.
3. Route content renders in the content outlet; the active nav item is
   highlighted.

## Functional Requirements

- Keep the shell composed of reusable layout pieces under
  `frontend/src/layout/` (`AppShell`, `Sidebar`, `Topbar`, `MobileNav`);
  no feature/domain logic inside the shell (ADR-006).
- Navigation is defined once in `frontend/src/app/navigation.ts` (`NAV_ITEMS`);
  sidebar, mobile nav and router all derive from it.
- Cover the mockup chrome: brand lockup + hamburger + page title/subtitle slot
  in the header, notification bell and account menu (avatar + name) on the right,
  a persistent "Withings · Conectado" status entry at the sidebar footer.
- Represent all MVP sections: Dashboard, Mediciones, Entrenamiento, Nutrición,
  Lista de compra, Progreso, Objetivos, Ajustes (nav already lists these).
- Desktop: fixed sidebar (`--sidebar-width`) + top bar (`--topbar-height`);
  mobile: collapsed sidebar with a compact bottom nav of the `primary` items and
  a "Más" overflow for the rest (matches the phone mockups).
- Shell must tolerate future auth state (authenticated/unauthenticated routes)
  without restructuring routing.

## Non-Functional Requirements

- No horizontal scroll on mobile; touch-friendly targets.
- Uses design tokens from `styles/theme.css` (no hardcoded colors).
- Deterministic, presentational; no data fetching in the shell.

## Data Model Notes

None — presentational shell. The account/notification areas are placeholders
until auth (future) and FOR-63 notifications land.

## Edge Cases

- Very small viewport → bottom nav stays usable, content area scrolls, header
  stays fixed.
- Unknown route → renders `NotFoundPage` inside the shell.
- Long page titles → truncate/wrap without breaking the header.

## Open Questions

- Account area is a placeholder (no auth yet, ADR-002 single-user). Confirm it
  stays a static avatar/name until an auth story exists — recommend yes.
- The header notification bell is a visual entry until FOR-63; document it as a
  non-functional placeholder for now.
