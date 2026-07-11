# FOR-60 AI Context

## Story

FOR-60 — Standardize loading, empty and error states
(https://dbhlab.atlassian.net/browse/FOR-60)

## Intent

Make the app feel reliable: consistent, actionable loading/empty/error states so
users don't read missing data as broken. Success is a reusable set of state
components adopted by every MVP feature.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (calm, no fake precision)
- `docs/adr/ADR-006-frontend.md` (error handling), `docs/adr/ADR-007-testing.md`
- `specs/FOR-50/` (design system), and the feature specs FOR-51..FOR-59
- Jira: https://dbhlab.atlassian.net/browse/FOR-60

## Domain Notes

- `frontend/src/components/PagePlaceholder.tsx` exists — fold it into the shared
  state components. Feature pages currently have ad-hoc states; standardize them.
- Error copy must be domain-aware and never expose raw exceptions.

## Architectural Constraints

- Reusable presentational components under `frontend/src/components/`, token-
  driven (FOR-50). No feature logic. Dev-only error detail behind a dev flag.

## Common Pitfalls

- Leaking technical/stack details to users.
- Conflating empty and error states.
- Layout jumps between loading and loaded.

## Suggested Implementation Order

1. Build the state components (page/widget loading, empty, filtered-empty,
   validation, recoverable error + retry, permission error).
2. Define copy guidelines (calm, actionable).
3. Migrate current feature pages to the shared components.
4. Tests (each state renders; retry works; no raw exception text).

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Verify feature pages use the shared states.
