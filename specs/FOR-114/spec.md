# FOR-114: Automated accessibility testing (jest-axe/axe-core)

Jira: https://dbhlab.atlassian.net/browse/FOR-114
Epic: FOR-47 UI & UX

## Summary

FOR-61 established accessible interaction patterns and FOR-112/FOR-113 fix
concrete heading/state issues, but nothing today automatically catches a
regression. Integrate `jest-axe`/`axe-core` into the existing
vitest + Testing Library setup (`frontend/package.json` already has
`vitest`, `@testing-library/react`, `@testing-library/jest-dom`,
`@testing-library/user-event` — no accessibility-testing library yet), add
axe assertions to representative screens, and ensure CI runs them.

## User/System Flow

No end-user-facing screen — a testing/tooling story.

## Functional Requirements

- Add `jest-axe` (or `vitest-axe`, whichever integrates more cleanly with
  the existing vitest + jsdom setup — see Open Questions) and `axe-core` as
  dev dependencies.
- Add a shared test helper (e.g. `frontend/src/test/axe.ts` alongside the
  existing `frontend/src/test/` setup) wrapping `axe(container)` +
  `toHaveNoViolations` so individual test files stay short.
- Add axe assertions to a representative set of screens covering the
  epic's main patterns: at least Dashboard (metric tiles), Mediciones or
  Progreso (charts), Lista de compra (tabs + modal), Ajustes (form
  sections), Onboarding (multi-step flow) and Integraciones (destructive
  confirmation dialog).
- Wire the new tests into the existing `npm run test` script so CI (once
  FOR-84 defines the workflow) picks them up automatically — no separate
  test command to remember to run.

## Non-Functional Requirements

- Axe scans run against rendered DOM output already produced by existing
  Testing Library renders — no new rendering harness, no browser
  automation (Playwright/Cypress) introduced; stays within ADR-007's
  "frontend tests for key rendering and interaction states" layer.
- Fast enough to run in the existing `npm run test` loop without materially
  slowing local development — scope the representative-screen set rather
  than scanning every component in isolation.

## Data Model Notes

None — testing infrastructure only.

## Edge Cases

- A screen with an intentionally-inert "Próximamente" control (e.g.
  `SecuritySection`'s 2FA/export entry points) — confirm axe doesn't flag
  the inert-but-visible pattern as a violation; if it does, document the
  suppression rationale rather than silently disabling the rule everywhere.
- Toast/notification content rendered via `aria-live` (FOR-63) — verify the
  axe scan runs after the relevant state settles, not against a stale
  snapshot mid-animation.

## Open Questions

- `jest-axe` (designed for Jest) vs. `vitest-axe`/direct `axe-core` +
  `jest-dom`'s `toHaveNoViolations` matcher registered manually — recommend
  verifying compatibility with the project's vitest version (`^3.0.0`)
  before committing to a package name; document the chosen package and any
  compatibility shim in `ai-context.md` during implementation.
- Whether axe assertions live inside each screen's existing `.test.tsx`
  file or a separate `*.a11y.test.tsx` file — recommend inline in the
  existing test files (keeps a11y coverage next to the behavior it
  protects) unless the existing files get unwieldy.
