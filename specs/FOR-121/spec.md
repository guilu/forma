# FOR-121: Onboarding answers persisted to backend

Jira: https://dbhlab.atlassian.net/browse/FOR-121
Epic: FOR-47 UI & UX

## Summary

`onboardingStorage.ts`'s doc comment documents the current state
explicitly: "No onboarding/profile/goals backend exists yet... this module
is the single place that reads/writes onboarding progress... isolated
behind a small read/write API... so swapping it for a real `PATCH
/api/v1/onboarding` call later touches one file, not every step." FOR-107
(backend) adds that persistence. This story does exactly the swap the
original design anticipated: save onboarding answers/profile/goals to the
backend, and change the first-run gate to read a backend flag instead of
the current `localStorage`-only check.

## User/System Flow

1. User completes (or partially completes) the onboarding flow
   (`/onboarding`); each step's answers save to the backend (FOR-107)
   instead of only `localStorage`.
2. On completion, the backend's `firstRunCompleted` flag is set.
3. A returning user's first-run detection (`OnboardingPage.tsx`'s doc
   comment: "there is no auth/user/profile backend yet... a manual route
   plus a local flag is enough for the MVP") now reads the backend flag;
   `localStorage` remains a fast local cache/fallback, not the source of
   truth.

## Functional Requirements

- Swap `onboardingStorage.ts`'s persistence calls for real
  `PATCH /api/v1/onboarding`-shaped calls against FOR-107's onboarding
  endpoint(s) — the module's doc comment explicitly anticipated this
  exact swap, "touches one file, not every step."
- Keep `localStorage` as a fast local draft/cache during the flow (so a
  mid-flow reload doesn't lose in-progress, not-yet-submitted answers),
  but persist authoritative progress to the backend at each step
  boundary or on completion (decide during implementation and document
  which).
- First-run gate: read `firstRunCompleted` from the backend (FOR-107)
  rather than only the local flag; `localStorage` flag becomes a
  fallback for when the backend call hasn't resolved yet, not the
  authoritative source.
- The body-metrics step's existing real persistence path (`MeasurementForm`
  via `BodyMetricsStep`, already hitting the real FOR-17 API) is
  unaffected — this story only changes the previously-local-only steps.
- No forced automatic redirect introduced — `OnboardingPage.tsx`'s doc
  comment explicitly defers that ("a destructive automatic redirect could
  trap a returning user with no way out... future work"); this story does
  not change that decision, only where the "already completed" signal
  comes from.

## Non-Functional Requirements

- No data loss on a mid-flow reload before the backend has ever been
  written to — the local draft must still work as an in-flight cache.
- Backend save failures during onboarding must not block the user from
  proceeding through the flow (a failed save should surface a
  non-blocking, calm message per FOR-63, matching the module's stated
  design goal of resilience).

## Data Model Notes

Consumes FOR-107's onboarding-answers persistence and
`firstRunCompleted` flag, shaped to match `OnboardingAnswers`/
`OnboardingProgress` (`onboardingStorage.ts`) — FOR-107 was explicitly
scoped to mirror these types.

## Edge Cases

- Backend unreachable mid-flow → local draft still lets the user continue
  through steps; a final "answers not saved to server yet" state is
  surfaced rather than silently losing progress or blocking navigation.
- User completes onboarding twice (e.g. revisits `/onboarding` after
  completion) → backend accepts the resubmission (FOR-107's Edge Cases:
  "onboarding answers re-submitted after completion → allowed, treated as
  a profile edit"), doesn't error.
- `localStorage` cleared mid-flow (private browsing, quota) → backend
  becomes the recovery source if any prior step already persisted;
  otherwise the flow restarts cleanly (same behavior as today's
  `localStorage`-only failure mode).

## Open Questions

- Whether persistence happens per-step (safer against loss, more network
  calls) or only on final completion (fewer calls, more risk of losing a
  long partial flow) — recommend per-step for resilience, given
  `onboardingStorage.ts`'s own design goal of not losing progress; document
  the final choice.
