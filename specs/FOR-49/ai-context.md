# FOR-49 AI Context

## Story

FOR-49 — Create responsive application shell
(https://dbhlab.atlassian.net/browse/FOR-49)

## Intent

Give every feature a predictable frame across desktop/tablet/mobile. Success is a
reusable, domain-agnostic shell (layout + header + primary nav + mobile nav +
content + account area) that already carries all MVP sections and matches the
chrome shared by every mockup.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (dark technical dashboard, main navigation list)
- `docs/adr/ADR-006-frontend.md` (centralized navigation, layout conventions),
  `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- Mockups: every `docs/*.png` shares the shell — see `docs/1-dashboard.png`
- Jira: https://dbhlab.atlassian.net/browse/FOR-49

## Domain Notes

- Repository already has the shell (FOR-81): `layout/AppShell.tsx`,
  `Sidebar.tsx`, `Topbar.tsx`, `MobileNav.tsx`, `app/navigation.ts` (`NAV_ITEMS`),
  `app/routes.tsx`. This story refines/hardens it — do not rebuild from scratch.
- Navigation labels are Spanish and match the mockups.

## Architectural Constraints

- Layout components under `frontend/src/layout/`; no feature logic in the shell.
- Single source of navigation (`NAV_ITEMS`); router + both navs derive from it.
- Consume `styles/theme.css` tokens only. Presentational — no data fetching.

## Common Pitfalls

- Coupling the shell to a specific domain module.
- Duplicating the nav list in more than one place.
- Breaking mobile with a fixed desktop sidebar (must collapse to bottom nav).

## Suggested Implementation Order

1. Audit the existing shell vs the mockup chrome (header actions, sidebar footer
   status, account area).
2. Add missing header pieces (title/subtitle slot, notifications + account area).
3. Confirm mobile bottom nav (primary items + "Más" overflow) and no h-scroll.
4. Layout/navigation tests (render, active item, mobile nav) with RTL + Vitest.

## Validation

Run `npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Visually review desktop and mobile against `docs/1-dashboard.png`.
