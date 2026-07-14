# FOR-113 UI Spec

## Screens

- Onboarding (`frontend/src/pages/onboarding/OnboardingPage.tsx`) at
  `/onboarding`.
- Lista de compra (`frontend/src/pages/ShoppingPage.tsx`) at
  `/lista-compra` — `ProductEditModal` nested flow.

## Components

- New page-level `<h1>` in `OnboardingPage`/`OnboardingStepShell`.
- `ProductEditModal` (nested in `ShoppingPage.tsx`) — inline
  loading/error/not-found paragraphs replaced by `LoadingState`,
  `ErrorState` and (recommended) `EmptyState`.
- `ErrorState` — one real caller now passes `detail`/`showDetail`.

## States

- Onboarding: unchanged step states, plus a stable page-level heading
  across steps.
- `ProductEditModal`: loading → shared `LoadingState`; error → shared
  `ErrorState` (no retry inside the modal); not-found → shared
  `EmptyState` (or `ErrorState` without retry, per the Open Question in
  `spec.md`).

## Interactions

- No new interactions — this story changes markup/semantics of existing
  states, not behavior.

## Accessibility

- Onboarding gains a proper page landmark heading (`<h1>`), fixing a
  missing top-level heading for screen reader users navigating by heading
  (WCAG 2.4.6).
- `ProductEditModal`'s migrated states inherit the shared components'
  existing `role="status"`/`role="alert"` semantics (FOR-60/FOR-61),
  replacing ad hoc paragraphs with less consistent semantics.

## Responsive Behavior

No change — purely semantic/markup cleanup, no layout impact.
