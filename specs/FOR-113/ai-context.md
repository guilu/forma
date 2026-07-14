# FOR-113 AI Context

## Story

FOR-113 — A11y & state-consistency cleanup
(https://dbhlab.atlassian.net/browse/FOR-113)

## Intent

Three small, previously-documented gaps, bundled into one cleanup story:
missing onboarding `<h1>`, a deliberately-deferred FOR-60 migration in
`ShoppingPage`'s `ProductEditModal`, and `ErrorState`'s unused dev-only
`detail`/`showDetail` escape hatch. None of these need new backend work or
new components — all three reuse what already exists.

## Blocked by

None.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` ("every major screen must handle loading,
  empty and error states" + "accessibility... first-class MVP concerns")
- `specs/FOR-60/` (shared loading/empty/error state components this story
  finishes adopting)
- `specs/FOR-61/` (accessible interaction patterns — focus/heading order)
- Jira: https://dbhlab.atlassian.net/browse/FOR-113

## Domain Notes

- `frontend/src/pages/onboarding/OnboardingPage.tsx` — no `<h1>` today
  (verified: `grep -rn "<h1" frontend/src/pages` finds it in every other
  page file except this one).
- `frontend/src/pages/ShoppingPage.tsx` lines ~235–246 — `ProductEditModal`'s
  doc comment explicitly documents the deferral this story resolves: "this
  modal's own loading/error/not-found states are deliberately left on
  their pre-existing inline markup... Follow-up: a future story can fold
  this into `EmptyState`/`ErrorState`."
- `frontend/src/components/ErrorState.tsx` — `detail`/`showDetail` props
  already exist (lines 30–33) but `grep -rn "showDetail"
  frontend/src --include="*.tsx"` outside `ErrorState.tsx`/its test finds
  no caller — genuinely dead code today.

## Architectural Constraints

- Reuse `LoadingState`/`ErrorState`/`EmptyState` exactly as they exist —
  no new shared components, no prop additions beyond what FOR-60 already
  ships (this story is adoption, not extension).
- `showDetail` must derive from a build-time dev flag (`import.meta.env.
  DEV`), never a runtime/user-toggleable setting (ErrorState.tsx's own
  contract).

## Common Pitfalls

- Collapsing `ProductEditModal`'s distinct not-found vs. error messages
  into one generic shared-component call — keep the two messages and their
  semantic distinction.
- Wiring `showDetail={true}` unconditionally instead of gating on
  `import.meta.env.DEV` — this would leak diagnostic detail to production
  users.
- Treating this as three separate PRs — it's one small story; keep the
  diff tight and reviewable as a single cleanup pass.

## Suggested Implementation Order

1. Add the onboarding page-level `<h1>` (check for heading-order fallout
   with FOR-112 if that story has already landed).
2. Migrate `ProductEditModal`'s three non-ready states to shared
   components.
3. Wire `detail`/`showDetail` into the migrated `ErrorState` call (or
   another suitable existing caller).
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Manually exercise onboarding (heading present) and Lista de
compra's product-edit modal (loading/error/not-found via shared
components; dev detail visible only in a dev build).
