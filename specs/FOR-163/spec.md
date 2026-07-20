# FOR-163 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-163
Epic: FOR-162 Design System v2. Root of the batch (blocks FOR-164).

## Summary

Fold the token set declared inline in the HTML mockup templates (`docs/*.html`) into the existing
design-token source of truth, so the app's colors, typography, spacing, radius and elevation match
the approved designs. Reconcile — do not fork. Frontend-only.

## Repository baseline (verified)

- Token source of truth: `frontend/src/styles/theme.css` — CSS custom properties under
  `:root,:root[data-theme='dark']` with a `[data-theme='light']` override; imported by
  `frontend/src/styles/global.css` (`@import './theme.css'`). Companion `frontend/src/theme/theme.ts`,
  `ThemeContext.tsx`, `ThemeToggle` (theming from FOR-62/FOR-81).
- Current dark tokens (excerpt): `--color-bg #050807`, `--color-surface #101613`, `--color-card #161f1a`,
  `--color-border #24342b`, `--color-accent #9dff57`, `--color-warning #ffd166`, `--color-danger #ff5f56`;
  spacing/radius/font scales as `--space-*` / `--font-*`.
- **The app uses CSS Modules, NOT Tailwind.** The templates use CDN Tailwind + a `tailwind.config`
  token block + Google Fonts (Montserrat, Be Vietnam Pro, Material Symbols) — these are references to
  translate into `theme.css` variables, NOT dependencies to adopt.
- Template token excerpt (dark): `primary-container #00b874`, `primary-fixed-dim #4cdf97`,
  `secondary-fixed-dim #7dde38`, `surface #10141a`, `surface-elevated #161B22`, `surface-stroke #30363D`,
  `tertiary #7ed8b6`, `error-container #93000a` (full set in each `docs/*.html` `tailwind.config`).

## User/System Flow

1. Extract the token values from the templates' `tailwind.config` blocks.
2. Map them onto the existing `theme.css` custom properties (add/adjust, single source of truth).
3. Define the light-theme counterparts (templates are dark-only) so `[data-theme='light']` stays valid.
4. Components/pages inherit the new values with no per-file changes (that work is FOR-164+).

## Functional Requirements

- Represent the template color / typography / spacing / radius / elevation tokens in `theme.css`
  (extend existing vars; add new ones only where the templates require them).
- Reconcile conflicting values (e.g. accent `#9dff57` vs template greens) to the approved template
  values; document each changed token.
- Keep light + dark themes resolving — provide light counterparts for every dark token touched.
- Typography: introduce the template fonts (Montserrat / Be Vietnam Pro) via the app's font strategy
  (self-hosted or bundled), wired to `--font-*`. **No CDN fonts, no CDN Tailwind in the app.**

## Non-Functional Requirements

- Single source of truth preserved — no hardcoded values escape into feature pages by this change.
- Contrast remains accessible in both themes (FOR-61).
- `DesignSystemExamples.tsx` remains the visual review surface.

## UI / States

- No feature-page changes here. Verify via `DesignSystemExamples` and existing component snapshots.

## Edge Cases

- A template token with no current equivalent → add a new variable (documented), do not overload an
  unrelated one.
- Dark-only template value with no obvious light counterpart → derive an accessible light value, document it.
- Existing consumers referencing a renamed/removed var → keep an alias or update consumers (prefer alias
  to keep this story token-only).

## Open Questions

- Font delivery: self-host vs the app's current `--font-sans` (confirm whether Montserrat/Be Vietnam Pro
  are already bundled; if not, add them without CDN).
- Whether to rename existing vars to the template's semantic names (e.g. `surface-elevated`) or map values
  onto the current names — default: keep current names, remap values, add aliases only if needed.
- Material Symbols icon font — in scope here (token/font wiring) or deferred to the components story (FOR-164)?
