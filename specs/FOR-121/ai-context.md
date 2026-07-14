# FOR-121 AI Context

## Story

FOR-121 — Onboarding answers persisted to backend
(https://dbhlab.atlassian.net/browse/FOR-121)

## Intent

`onboardingStorage.ts` was deliberately built as a single, isolated
read/write boundary specifically so this swap would be cheap: "isolated
behind a small read/write API... so swapping it for a real `PATCH
/api/v1/onboarding` call later touches one file, not every step." FOR-107
ships that backend; this story is the anticipated swap.

## Blocked by

FOR-107 (backend: onboarding answers persistence + `firstRunCompleted`
flag). Do not start until FOR-107's onboarding endpoint(s) exist.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-002-authentication.md` (single-user MVP context this story
  operates within — first-run detection still has no session to key on)
- `specs/FOR-107/spec.md` (the onboarding persistence + flag this story
  consumes, explicitly scoped to mirror `OnboardingAnswers`/
  `OnboardingProgress`)
- `specs/FOR-59/` if present, otherwise `frontend/src/pages/onboarding/
  OnboardingPage.tsx`'s doc comment (first-run detection rationale,
  explicitly says "future work once a real session/user story exists" —
  this story is that future work, for persistence; full session-based
  redirect logic is still out of scope per that same doc comment)
- Jira: https://dbhlab.atlassian.net/browse/FOR-121

## Domain Notes

- `frontend/src/pages/onboarding/onboardingStorage.ts` — read the full
  doc comment; it explicitly anticipates this exact story ("swapping it
  for a real `PATCH /api/v1/onboarding` call later touches one file").
  `OnboardingAnswers`/`OnboardingProgress` types define the exact shape to
  persist.
- `frontend/src/pages/onboarding/OnboardingPage.tsx` — doc comment
  explains current first-run detection relies purely on a local flag
  because there's no backend; this story changes the flag's source, not
  the "no forced redirect" decision (that stays deferred, per the same
  comment).
- The body-metrics step already persists for real via `MeasurementForm`
  (FOR-17 API) — do not touch that path; this story is about the
  remaining steps that were local-only.

## Architectural Constraints

- Keep `onboardingStorage.ts` as the single persistence boundary — do not
  scatter new backend calls across individual step components (the
  module's own design goal).
- Local draft (`localStorage`) remains as a resilience layer, not removed
  outright — losing in-progress answers on a network blip would be a
  regression, not an improvement.

## Common Pitfalls

- Making onboarding steps block on a slow/failed backend call instead of
  saving locally first and syncing in the background.
- Forcing an automatic redirect for returning users as part of this story
  — `OnboardingPage.tsx`'s doc comment explicitly defers that decision;
  don't fold it in here without being asked.
- Losing the body-metrics step's existing FOR-17 persistence path while
  refactoring the surrounding storage module.

## Suggested Implementation Order

1. Confirm FOR-107 has shipped; extend `onboardingStorage.ts`'s
   read/write functions to call the backend, keeping `localStorage` as a
   cache/fallback.
2. Update the first-run gate to read the backend flag (with local fallback
   while the backend call is in flight).
3. Add non-blocking save-failure feedback (FOR-63 pattern).
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Walk through onboarding end to end against a local/dev
backend once FOR-107 is available; verify resumption after a reload and
graceful degradation with the backend stopped.
