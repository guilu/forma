# FOR-62: Add dark mode support

Jira: https://dbhlab.atlassian.net/browse/FOR-62
Epic: FOR-47 UI & UX

## Summary

Add full light/dark theme support: theme token mapping, a user theme toggle,
persisted preference, system-preference fallback, and correct chart/card/status
rendering in both themes. `styles/theme.css` already defines dark (default) +
`[data-theme='light']` token overrides (FOR-81); this story adds the **toggle,
persistence and system fallback** and verifies every surface in both themes.

## User/System Flow

1. On load, resolve theme: explicit stored preference → else system preference →
   else dark default; set `data-theme` on the root.
2. User toggles theme (in Ajustes, FOR-58); preference persists.
3. All components re-render correctly via tokens (no per-theme component styles).

## Functional Requirements

- **Light + dark themes** driven entirely by the `theme.css` token overrides.
- **Theme resolution**: explicit preference > system (`prefers-color-scheme`) >
  dark default; applied by setting `data-theme` on `:root`.
- **Toggle**: a control (Ajustes) to switch light/dark (and optionally "system").
- **Persistence**: store the preference (localStorage) and restore on load,
  before first paint where possible (avoid flash).
- **Correct rendering**: charts (`LineChart`), cards, status badges, focus states
  correct in both themes (no hardcoded colors).

## Non-Functional Requirements

- No duplicated per-theme component styles — tokens only.
- Contrast remains acceptable in both themes (aligns with FOR-61).
- No theme flash on load where avoidable.

## Data Model Notes

Uses FOR-50 / FOR-81 tokens. Theme preference persistence is client-side
(localStorage) — no backend needed. If a user-preferences backend later exists
(FOR-58), the stored theme can migrate there; document.

## Edge Cases

- No stored preference + no system signal → dark default.
- System preference changes at runtime → follow it only when in "system" mode.
- Any component using a hardcoded color → fix to a token (regression risk).

## Open Questions

- Offer an explicit "system" option vs only light/dark — recommend light/dark +
  system fallback for the MVP; document.
- Where the toggle lives — recommend Ajustes (FOR-58); document.
