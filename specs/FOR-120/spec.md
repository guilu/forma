# FOR-120: Theme preference persisted server-side

Jira: https://dbhlab.atlassian.net/browse/FOR-120
Epic: FOR-47 UI & UX

## Summary

`ThemeContext.tsx`/`theme.ts` (FOR-62) currently resolve the theme entirely
client-side: explicit `localStorage` preference (`forma.theme` key) >
system (`prefers-color-scheme`) > dark default, with a pre-paint inline
script in `index.html` mirroring the same precedence to avoid a flash.
FOR-107 (backend) adds a theme preference field. This story reads the
initial theme from that backend preference, falls back to
system/`localStorage` when unavailable, persists changes back to the
backend, and preserves the existing no-flash behavior.

## User/System Flow

1. On load, the pre-paint inline script still applies the best guess it
   can without a network call (localStorage/system/dark), preserving the
   no-flash guarantee.
2. Once the app mounts, `ThemeProvider` fetches the persisted preference
   from `GET /api/v1/profile` (FOR-107) and reconciles: if the backend
   preference differs from what was applied pre-paint, update
   `data-theme` accordingly (a single, cheap re-application, not a visible
   flash under normal conditions since it typically confirms the same
   value).
3. User toggles theme (`ThemeToggle`, FOR-62); the new mode is applied
   immediately (unchanged local behavior) and also persisted to the
   backend via FOR-107's update endpoint.

## Functional Requirements

- `ThemeProvider` (`frontend/src/theme/ThemeContext.tsx`) reads the
  backend theme preference on mount (in addition to, not instead of, the
  existing `readStoredThemeMode()`/system-preference logic).
- Precedence: backend-persisted preference (once loaded) becomes the
  source of truth; until it loads, fall back to the existing
  `localStorage` > system > dark chain exactly as today, so first paint is
  unaffected by network latency.
- `setMode` (theme.ts) persists the new mode to the backend
  (FOR-107's update endpoint) in addition to its existing
  `storeThemeMode()` `localStorage` write — keep the `localStorage` write
  as a fast local fallback/offline cache, not a replacement.
- No-flash behavior preserved: `index.html`'s pre-paint inline script is
  unchanged (it cannot make a network call before paint); only the
  post-mount reconciliation step is new.
- Backend persistence failure (network error, FOR-107 endpoint down) →
  theme toggle still works locally (`localStorage`-only, degraded mode);
  surface a non-blocking error or silently degrade, consistent with the
  cosmetic-preference precedent already documented in `theme.ts`
  ("Preference simply won't survive a reload — not worth surfacing an
  error for a cosmetic preference").

## Non-Functional Requirements

- Zero regression to FOR-62's existing no-flash guarantee and "system
  preference changes at runtime → follow it only when in 'system' mode"
  behavior.
- The backend call is non-blocking for first paint — theme must never wait
  on a network round-trip to render.

## Data Model Notes

Consumes FOR-107's theme preference field (`ThemeMode`-shaped:
`light | dark | system`, matching `frontend/src/theme/theme.ts`'s existing
type exactly, since FOR-107 was scoped to mirror it).

## Edge Cases

- Backend preference unavailable/not-yet-created (first run before FOR-107
  has ever been written to) → falls back to FOR-107's documented default
  (dark), consistent with `theme.ts`'s existing dark default.
- Backend and `localStorage` disagree (e.g. user changed theme on another
  device/session) → backend value wins once loaded, `localStorage` is
  updated to match (keeps the fast local fallback in sync).
- Backend request fails after a local toggle → local UI state does not
  revert (the toggle already applied instantly); only persistence failed,
  not the visible change — matches the existing cosmetic-preference
  tolerance.

## Open Questions

- Whether the post-mount reconciliation should ever visibly change the
  theme after paint (if backend disagrees with the pre-paint guess) — this
  is expected to be rare (same device, same browser, matching
  `localStorage`) but must be handled without a jarring flash if it
  happens; consider a very brief, deliberate transition rather than an
  instant snap, or accept the snap as acceptable for the rare case.
