# FOR-114 AI Context

## Story

FOR-114 — Automated accessibility testing (jest-axe/axe-core)
(https://dbhlab.atlassian.net/browse/FOR-114)

## Intent

FOR-61 (accessible interaction patterns), FOR-112 (heading hierarchy) and
FOR-113 (a11y cleanup) all fix accessibility issues by hand, one story at a
time, with nothing to stop a future story from regressing them. This story
adds automated axe scans to the existing test suite so accessibility
regressions get caught the same way a broken render or a failed assertion
would.

## Blocked by

None.

## Relevant Documents

- `AGENTS.md`
- `docs/adr/ADR-007-testing.md` ("Frontend tests for key rendering and
  interaction states")
- `docs/adr/ADR-006-frontend.md` ("Accessibility and responsive behavior
  are first-class MVP concerns")
- `specs/FOR-61/` (the interaction patterns axe should protect)
- `specs/FOR-112/` (the heading-hierarchy fix axe's "heading-order" rule
  should cover)
- `frontend/package.json` — current test tooling: `vitest ^3.0.0`,
  `@testing-library/react ^16.1.0`, `@testing-library/jest-dom ^6.6.3`,
  `@testing-library/user-event ^14.5.2`; no accessibility-testing package
  yet
- Jira: https://dbhlab.atlassian.net/browse/FOR-114

## Domain Notes

- No `jest-axe`/`axe-core`/`vitest-axe` dependency exists in
  `frontend/package.json` today — verified directly against the file.
- `frontend/src/test/` already holds shared test setup — the new axe
  helper belongs alongside it, not duplicated per test file.
- `npm run test` and `npm run test:watch` are the existing vitest entry
  points (`frontend/package.json` scripts) — new axe assertions should run
  through the same command, not a separate one.

## Architectural Constraints

- Stays within the existing Testing Library render harness — no new
  browser-automation tool (Playwright/Cypress) introduced by this story.
- Axe assertions are test code only — never shipped in application bundles.

## Common Pitfalls

- Choosing a package incompatible with vitest 3 (some `jest-axe` versions
  assume Jest's global matcher registration style) — verify compatibility
  before committing; `vitest-axe` or manually registering
  `toHaveNoViolations` via `expect.extend` are both viable fallbacks.
- Scanning too many components in isolation instead of a representative
  set of full screens — this bloats test time without meaningfully
  increasing coverage of real user-facing structure.
- Suppressing a real violation with a rule-disable comment instead of
  fixing the underlying markup, when the fix is small (e.g. missing label).

## Suggested Implementation Order

1. Choose and install the axe package; verify it runs under vitest.
2. Build the shared `axe()` + `toHaveNoViolations` test helper.
3. Prove the helper works against one deliberately-broken and one
   compliant fixture.
4. Add axe assertions to the representative screen set listed in
   `spec.md`.
5. Confirm `npm run test` runs them; document any CI wiring needed once
   FOR-84 exists.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck` from `frontend/`.
Deliberately reintroduce a known violation (e.g. remove an `aria-label`)
locally to confirm the new tests actually fail, then revert.
