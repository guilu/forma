# FOR-61 AI Context

## Story

FOR-61 — Implement accessible interaction patterns
(https://dbhlab.atlassian.net/browse/FOR-61)

## Intent

Improve usability for everyone and avoid accessibility debt before the UI grows.
Success is keyboard-usable core flows, visible focus, semantic structure,
accessible forms/controls, acceptable contrast and announced status messages.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (high-contrast intent), `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- All feature specs FOR-49..FOR-60 (this pass touches them)
- Jira: https://dbhlab.atlassian.net/browse/FOR-61

## Domain Notes

- Cross-cutting: shell (`layout/*`), forms (`MeasurementForm`), modal
  (`Modal.tsx`), state components (FOR-60), design system (FOR-50).
- Semantic HTML first; ARIA only where semantics are insufficient.

## Architectural Constraints

- Reuse focus/contrast tokens from `styles/theme.css`. No inaccessible custom
  controls. Keep changes presentational/markup-level.

## Common Pitfalls

- Color-only status; missing focus states; unlabelled icon buttons.
- Custom controls that trap or skip keyboard focus.
- Poor focus management on route/modal/step changes.

## Suggested Implementation Order

1. Audit landmarks/headings and keyboard paths for core flows.
2. Fix labels/errors, focus visibility, icon-button names.
3. Verify contrast (accent text usage); add `aria-live` to state/feedback.
4. Add automated a11y checks where practical; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Keyboard-only walkthrough + screen-reader spot checks on desktop and
mobile.
