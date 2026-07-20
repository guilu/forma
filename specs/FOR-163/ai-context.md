# FOR-163 AI Context

## Story

FOR-163 — Design tokens: reconcile HTML template tokens with the design system. Frontend-only. Root of
the FOR-162 batch; blocks the shared-components story (FOR-164).

## Intent

Make `theme.css` reflect the approved template palette/type/spacing/radius/elevation, so every component
and page inherits the right look once FOR-164 consumes it. Pure token work — no component/page restyle here.

## Relevant Documents

- Templates: `docs/1-dashboard.html` … `docs/4-nutricion.html` (inline `tailwind.config` token blocks).
- `docs/ui-guidelines.md` — current palette rationale.
- `specs/FOR-50/` design system, FOR-62 theming, FOR-81 token bootstrap.
- `AGENTS.md` — tokens are the single source of truth; no hardcoded visual rules in pages.
- Jira: https://dbhlab.atlassian.net/browse/FOR-163

## Repo Notes (verified)

- `frontend/src/styles/theme.css` — CSS custom props, dark default + `[data-theme='light']`; imported by
  `global.css`. `frontend/src/theme/theme.ts` + `ThemeContext` + `ThemeToggle`.
- App is **CSS Modules**, no Tailwind. Templates' Tailwind config is a reference to translate, not adopt.
- `DesignSystemExamples.tsx` is the showcase for visual verification.
- Current accent `#9dff57`; templates use `#00b874`/`#4cdf97`/`#7dde38` etc. Reconcile to template values.

## Architectural Constraints

- Frontend-only, token layer only. Do not restyle components/pages (that is FOR-164+).
- No CDN Tailwind, no CDN fonts in the app — translate to `theme.css` vars + bundled fonts.
- Keep light + dark valid; keep contrast accessible (FOR-61).
- Single source of truth: values live in `theme.css`, not in feature CSS modules.

## Common Pitfalls

- Adopting CDN Tailwind or CDN Google Fonts because the templates use them.
- Updating dark tokens only and leaving `[data-theme='light']` stale/broken.
- Overloading an existing variable with an unrelated template value instead of adding a new one.
- Letting a hardcoded hex slip into a component/page instead of a variable.

## Suggested Implementation Order

1. Extract + tabulate the template tokens (color/type/space/radius/elevation) from the `tailwind.config` blocks.
2. Map onto `theme.css` dark vars; add new vars as needed (+ document each change).
3. Define/adjust the `[data-theme='light']` counterparts.
4. Wire the template fonts via the app font strategy (bundled, not CDN); verify via `DesignSystemExamples`.

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm both themes
resolve, `DesignSystemExamples` reflects the new palette, contrast is acceptable, and no hardcoded hex
leaked into feature files. No CDN Tailwind/fonts added.
