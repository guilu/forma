# FOR-120 AI Context

## Story

FOR-120 — Theme preference persisted server-side
(https://dbhlab.atlassian.net/browse/FOR-120)

## Intent

FOR-62 built theme resolution entirely client-side because no backend
existed. FOR-107 adds a theme preference field to the new profile/
preferences backend. This story wires the two together without regressing
FOR-62's carefully-built no-flash guarantee, which depends on a pre-paint
inline script that cannot make a network call.

## Blocked by

FOR-107 (backend: theme preference field on the profile/preferences
aggregate). Do not start until FOR-107's `ThemeMode`-shaped field is
available.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md`
- `specs/FOR-107/spec.md` (theme field this story consumes — explicitly
  scoped to mirror `ThemeMode`)
- `specs/FOR-62/` if present, otherwise read `frontend/src/theme/
  ThemeContext.tsx` and `frontend/src/theme/theme.ts` directly — both are
  extensively doc-commented with the existing precedence rules and the
  no-flash mechanism
- Jira: https://dbhlab.atlassian.net/browse/FOR-120

## Domain Notes

- `frontend/src/theme/theme.ts` — `ThemeMode` type
  (`'light' | 'dark' | 'system'`), `THEME_STORAGE_KEY = 'forma.theme'`,
  `readStoredThemeMode`/`storeThemeMode`/`systemPrefersLight` — the exact
  functions this story extends (persistence) or reads for fallback
  behavior (unchanged).
- `frontend/src/theme/ThemeContext.tsx` — `ThemeProvider`'s doc comment
  explicitly explains the no-flash mechanism: `index.html` has an inline
  script mirroring this module's precedence "synchronously before this
  component (and React) ever runs." That script is out of scope for this
  story — it cannot call the backend before paint.
- `index.html`'s inline script (not this story's to change) — keep its
  existing localStorage/system/dark precedence exactly as-is; this story
  only adds a post-mount reconciliation layer on top.

## Architectural Constraints

- The pre-paint script stays network-free — this is a hard constraint, not
  a preference; violating it reintroduces the flash FOR-62 was built to
  avoid.
- Backend persistence failure must never block or revert the visible theme
  change — theme is explicitly treated as a low-stakes cosmetic preference
  (`theme.ts`'s own doc comment on `storeThemeMode`).

## Common Pitfalls

- Making the backend fetch blocking (awaiting it before first render) —
  this reintroduces a flash/delay FOR-62 specifically avoided.
- Changing `index.html`'s inline script to call the backend — it runs
  before React/fetch infrastructure is available in the intended fast
  path; keep it local-only.
- Treating a backend persistence failure as a reason to revert the user's
  already-applied local toggle.

## Suggested Implementation Order

1. Confirm FOR-107's theme field is available; add a read call in
   `ThemeProvider` on mount.
2. Reconcile backend value vs. pre-paint value post-mount.
3. Extend `setMode`'s persistence to also write to the backend, keeping
   the existing `localStorage` write.
4. Tests per `tests.md`, especially the no-flash regression guard.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Manually verify: hard reload never flashes the wrong theme;
toggling persists across a reload even with `localStorage` cleared (once a
backend preference exists); backend outage doesn't block theme toggling.
