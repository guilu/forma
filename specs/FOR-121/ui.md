# FOR-121 UI Spec

## Screens

- Onboarding (`frontend/src/pages/onboarding/OnboardingPage.tsx` and its
  `steps/`) at `/onboarding`.

## Components

- `onboardingStorage.ts` — persistence layer swapped from `localStorage`-
  only to backend-backed (FOR-107), per its own anticipated design.
- `OnboardingPage.tsx` — first-run detection reads the backend flag instead
  of only the local one; no visual/component change otherwise.

## States

- Existing step states unchanged. New: a non-blocking "not saved yet"
  indicator if a backend save fails mid-flow (reuses FOR-63 patterns —
  inline message or toast, not a blocking error screen).

## Interactions

- Step navigation (Next/Back) unchanged; each step boundary now also
  triggers (or queues) a backend save, per the Open Question in `spec.md`.

## Accessibility

- Any new non-blocking save-failure message follows FOR-63/FOR-61's
  `aria-live` announcement pattern, consistent with the rest of the app.

## Responsive Behavior

No change — this story is persistence-only, not a layout change.
