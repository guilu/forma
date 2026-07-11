# FOR-62 AI Context

## Story

FOR-62 — Add dark mode support
(https://dbhlab.atlassian.net/browse/FOR-62)

## Intent

Comfortable evening use via a proper light/dark theme. Success is a token-driven
theme with a toggle, persisted preference and system fallback, rendering every
surface correctly in both modes.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (palette), `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-50/` (design system), `specs/FOR-58/` (settings toggle),
  `specs/FOR-61/` (contrast)
- Jira: https://dbhlab.atlassian.net/browse/FOR-62

## Domain Notes

- `frontend/src/styles/theme.css` already maps tokens for dark (default) and
  `:root[data-theme='light']`. This story adds the runtime toggle + persistence +
  system fallback, not the token palette.
- `data-theme` on `:root` is the switch mechanism.

## Architectural Constraints

- Tokens only — no per-theme component styles (ADR-006). Persist client-side
  (localStorage). Apply the theme before first paint to avoid flash.

## Common Pitfalls

- Hardcoded colors in components breaking one theme.
- Theme flash on load.
- Following system changes even after the user picked an explicit theme.

## Suggested Implementation Order

1. Theme resolver: stored preference > system > dark; set `data-theme` early.
2. Theme context/hook + toggle control (Ajustes, FOR-58).
3. Persist + restore preference; audit components for hardcoded colors.
4. Verify charts/cards/badges/focus in both themes; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Visually review both themes across screens.
