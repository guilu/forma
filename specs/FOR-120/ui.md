# FOR-120 UI Spec

## Screens

- Cross-cutting — theme applies globally via `data-theme` on `<html>`; the
  user-facing control remains `ThemeToggle` (topbar, FOR-62) and
  `ProfileSection`'s theme row (Ajustes).

## Components

- `ThemeProvider`/`useTheme` (`frontend/src/theme/ThemeContext.tsx`) —
  gains a backend read on mount and a backend write on `setMode`, no
  change to its public interface (`mode`, `resolvedTheme`, `setMode`)
  consumed by `ThemeToggle`/`ProfileSection`.
- `theme.ts` — `setMode`'s persistence call extended to also write to the
  backend; `readStoredThemeMode`/`systemPrefersLight` unchanged (still the
  pre-paint/fallback path).

## States

- No new visual states — this story changes where the preference is
  sourced/persisted, not how `ThemeToggle` looks or behaves.

## Interactions

- Unchanged from the user's perspective: toggling still applies instantly.
  The only new interaction: on load, if the backend preference differs
  from the pre-paint guess, the theme may update once, shortly after
  mount (see `spec.md` Open Questions).

## Accessibility

- No change — `ThemeToggle`'s existing accessible name/state announcement
  (FOR-62) is unaffected by where the preference is persisted.

## Responsive Behavior

No change.
