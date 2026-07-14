# FOR-120 Test Plan

## Scope

Verify the theme provider reads the backend preference on mount without
breaking the no-flash guarantee, persists changes to the backend, and
degrades gracefully when the backend is unavailable.

## Domain Tests

N/A — frontend-only story.

## Application Tests

N/A — frontend-only story.

## API Tests

N/A — consumes the FOR-107 contract; no backend change in this story.

## UI Tests

- On mount, `ThemeProvider` fetches the backend theme preference and, if
  it differs from the pre-paint value, reconciles `data-theme` and
  internal state to match.
- `setMode` persists the new mode via the FOR-107 update endpoint in
  addition to `localStorage` (regression guard: existing
  `themedRendering.test.tsx`-style dual-theme rendering tests still pass).
- Backend fetch failure on mount → theme still resolves via the existing
  `localStorage`/system/dark fallback chain, no crash, no blocked render.
- Backend persistence failure on `setMode` → the local theme change still
  applies visually; failure does not revert or block the UI.
- `system` mode still follows live OS preference changes exactly as before
  (FOR-62 regression guard) when a backend preference of `system` is
  loaded.

## Edge Cases

- Backend preference not yet created (first run) → defaults consistent
  with `theme.ts`'s existing dark default, no error surfaced to the user.
- Backend and `localStorage` disagreeing → backend wins once loaded;
  `localStorage` updated to match.

## Fixtures

- Mocked `GET /api/v1/profile` responses: matching preference, differing
  preference, missing/first-run default, and a network failure.
