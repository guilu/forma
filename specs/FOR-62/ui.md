# FOR-62 UI Spec

## Screens

- Cross-cutting theming for all screens; the toggle lives in Ajustes (FOR-58).

## Components

- Theme provider/hook that resolves + applies `data-theme` on `:root`.
- Theme toggle control (light / dark / system) in settings.
- Consumes existing `styles/theme.css` token overrides (no new palette).

## States

- Loading: theme applied before first paint (no flash).
- Empty: no preference → system or dark default.
- Error: N/A.
- Success: chosen theme applied and persisted.

## Interactions

- Toggling updates `data-theme`, persists to localStorage, re-renders via tokens.
- "System" mode follows `prefers-color-scheme` live.

## Accessibility

- Toggle is a labelled control with visible focus; state announced.
- Contrast verified in both themes (with FOR-61).

## Responsive Behavior

- Theme is orthogonal to layout; both themes work at every breakpoint. The toggle
  is reachable in the mobile settings list.
