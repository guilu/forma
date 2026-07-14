# FOR-113: A11y & state-consistency cleanup

Jira: https://dbhlab.atlassian.net/browse/FOR-113
Epic: FOR-47 UI & UX

## Summary

Three small, unrelated-but-grouped cleanup items surfaced as documented
gaps in existing code: (1) `OnboardingPage` has no page-level `<h1>` —
every other page (`DashboardPage`, `MeasurementsPage`, `TrainingPage`,
`NutritionPage`, `ShoppingPage`, `ProgressPage`, `SettingsPage`,
`IntegrationsPage`) does; (2) `ShoppingPage`'s `ProductEditModal` nested
loading/error/not-found states were deliberately left on inline markup
during FOR-60 ("a documented deferral... Follow-up: a future story can fold
this into `EmptyState`/`ErrorState`") — this story is that follow-up; (3)
`ErrorState`'s dev-only `detail`/`showDetail` props exist but are never
passed by any caller today — wire them into a real caller so the
diagnostic escape hatch is actually usable in development.

## User/System Flow

No new screen — three targeted fixes across existing screens:
1. Onboarding (`/onboarding`) gains a page-level heading.
2. Lista de compra's product-edit modal states migrate to shared
   components.
3. One (or more) existing `ErrorState` caller passes `detail`/`showDetail`
   gated by a dev flag.

## Functional Requirements

- **Onboarding `<h1>`**: add a page-level `<h1>` to `OnboardingPage.tsx`
  (or `OnboardingStepShell.tsx`, wherever the step chrome renders),
  matching the pattern every other page already uses
  (`<h1 className={styles.title}>...</h1>`). Keep any existing step-level
  heading (if `OnboardingStepShell` renders one) at a lower level so the
  hierarchy doesn't skip (pairs with FOR-112).
- **ProductEditModal state migration**: replace the inline
  `loading`/`error`/`not-found` paragraphs in `ShoppingPage.tsx`'s
  `ProductEditModal` (lines ~321–335) with `LoadingState` (loading),
  `ErrorState` (error, no retry needed inside a modal — closing and
  reopening is the retry path) and a not-found treatment — reusing
  `EmptyState` with a title, or `ErrorState` without a retry action, per
  whichever shared component's semantics fit best; document the choice.
- **ErrorState dev-only detail**: pick a real, currently-failing-prone
  caller (e.g. `ShoppingPage`'s list-load error, or `ProductEditModal`'s
  own error state once migrated) and pass `detail={error.message}`
  `showDetail={import.meta.env.DEV}` so the escape hatch the component
  already supports is actually exercised somewhere in the app, not dead
  code.

## Non-Functional Requirements

- No visual regression on the migrated `ProductEditModal` states beyond
  adopting the shared components' existing look (already used elsewhere,
  e.g. `ShoppingPage`'s own page-level `LoadingState`/`ErrorState`).
- `detail` must never render in production — `showDetail` stays gated by a
  dev-only flag (`import.meta.env.DEV`), never a runtime toggle exposed to
  users (ErrorState.tsx doc comment: "never in production").

## Data Model Notes

None — presentational-only changes; no API or domain change.

## Edge Cases

- Onboarding step transitions must not lose the new page-level `<h1>`
  (i.e. it should persist across steps, not reset/refocus in a way that's
  disruptive — verify with FOR-61's focus-management patterns).
- `ProductEditModal`'s not-found case (product id no longer resolves) must
  remain distinguishable from its error case (network/API failure) after
  migration — don't collapse both into one generic message.
- `showDetail` accidentally left `true` in a production build → caught by
  code review / a lint rule is out of scope here; rely on
  `import.meta.env.DEV` evaluating `false` in production builds (Vite
  behavior).

## Open Questions

- Whether the not-found case reuses `EmptyState` (title-only, no retry) or
  `ErrorState` (message, no `onRetry` passed) — recommend `EmptyState`
  since "not found" isn't a recoverable-by-retry failure; document the
  final choice in `ProductEditModal`'s doc comment.
