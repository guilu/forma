# FOR-62 Test Plan

## Scope

Verify theme resolution, toggle, persistence, system fallback and correct
rendering in both themes.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — client-side preference only.

## UI Tests

- Default (no preference, no system signal) resolves to dark.
- The toggle switches `data-theme` between light and dark.
- The preference persists and is restored on reload.
- With no explicit preference, the system preference is followed.
- Core components (card, badge, chart container, focus) render via tokens in both
  themes (no hardcoded color regressions).

## Edge Cases

- System change at runtime followed only in "system" mode.
- Explicit choice overrides system.

## Fixtures

- Mocked `matchMedia(prefers-color-scheme)`; a localStorage stub; a couple of
  token-driven components rendered under each theme.
