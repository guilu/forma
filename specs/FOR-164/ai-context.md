# FOR-164 AI Context

## Story

FOR-164 — Shared UI components: align cards, badges, buttons and chart containers with the templates.
Frontend-only. Blocked by FOR-163 (tokens); blocks the four per-view stories.

## Intent

Fold the template look into the shared component library so a single update propagates to every feature
page. Preserve component APIs; consume the reconciled tokens.

## Relevant Documents

- Templates `docs/*.html`; `specs/FOR-163/` (tokens); `specs/FOR-50/` design system.
- FOR-60 (state components), FOR-61 (a11y), FOR-63 (notifications), FOR-112 (`Card` `headingLevel`).
- `AGENTS.md` — no hardcoded visual rules; tokens are the source of truth.
- Jira: https://dbhlab.atlassian.net/browse/FOR-164

## Repo Notes (verified)

- `frontend/src/components/` CSS-module components (list in spec.md). `DesignSystemExamples.tsx` = showcase.
- `Card` has `headingLevel`; keep it. FOR-60 states + `NotificationProvider` already shared.
- App is CSS Modules, no Tailwind — style with tokens in the component modules.

## Architectural Constraints

- Frontend-only; touch shared components, NOT feature pages (FOR-165..168 do those).
- Tokens only (FOR-163) — no hardcoded colors/spacing; no per-page overrides introduced.
- Preserve/extend APIs; never break existing call sites.
- Preserve a11y (FOR-61): focus, contrast, semantics; keep both themes correct.

## Common Pitfalls

- Breaking a component API (renaming/removing props) that pages depend on.
- Hardcoding template hex/px instead of using the FOR-163 vars.
- Leaving per-page CSS-module overrides that now fight the component (surface them for the per-view story).
- Re-baselining snapshots without reviewing the intentional visual diff.
- Regressing focus-visible / contrast while restyling.

## Suggested Implementation Order

1. Card/MetricCard (elevation/stroke/radius/typography) + `DesignSystemExamples` update (+ tests).
2. Badge/StatusPill tones; Button variants (+ tests).
3. ChartContainer/LineChart/MacroRing framing (+ tests).
4. Re-check FOR-60 states + notifications for visual consistency (+ a11y pass).

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm components
match the templates via `DesignSystemExamples`, APIs unchanged (pages still compile), both themes render,
focus/contrast preserved. Snapshot diffs reviewed, not blind-accepted.
