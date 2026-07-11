# FOR-59 AI Context

## Story

FOR-59 — Create first-run onboarding flow
(https://dbhlab.atlassian.net/browse/FOR-59)

## Intent

Help new users provide the minimum for useful training/nutrition/body guidance.
Success is a short, skippable, resumable multi-step flow ending with a clear next
action — no medical claims.

## Relevant Documents

- `AGENTS.md` (bootstrap: no onboarding/profile/goals backend)
- `docs/ui-guidelines.md`, `docs/7-objetivos.png` (goal concept)
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-002-*` (single-user MVP),
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-52/` (body entry), `specs/FOR-57/` (integration prompt),
  `specs/FOR-58/` (profile/preferences)
- Jira: https://dbhlab.atlassian.net/browse/FOR-59

## Domain Notes

- No backend for onboarding/profile/goals yet — design for future persistence;
  store step progress locally/in-memory in the MVP.
- Reuse `MeasurementForm` (FOR-52) for the metrics step and the FOR-57 connect
  prompt for the integration step.

## Architectural Constraints

- Multi-step UI flow with small, validated steps (ADR-006). No medical/diagnosis
  language. Reuse FOR-50 primitives + FOR-60 states + FOR-63 feedback.

## Common Pitfalls

- Blocking users on skippable steps.
- Building persistence with no backend instead of deferring it.
- Medical/diagnostic copy.

## Suggested Implementation Order

1. Step scaffold + progress + back/next/skip + local progress storage.
2. Profile, body metrics (reuse FOR-52), goal selection steps.
3. Training availability, equipment, nutrition basics, integration prompt.
4. Per-step validation; final "next action"; mobile review; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Walk the flow on desktop + mobile.
