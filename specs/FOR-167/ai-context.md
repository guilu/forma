# FOR-167 AI Context

## Story

FOR-167 — Entrenamiento view: apply mockup template layout (`docs/3-entrenamiento.html`). Frontend-only
visual refactor. Blocked by FOR-164. Coordinates with FOR-158/FOR-161.

## Intent

Restyle the training view to the approved template using refreshed components/tokens, preserving data,
completion actions and the streak/muscle-map behaviour.

## Relevant Documents

- Template `docs/3-entrenamiento.html`; `specs/FOR-163/`, `specs/FOR-164/`.
- `specs/FOR-53/` (training screens), `specs/FOR-143/` (streak/history), `specs/FOR-160/` (muscle-map),
  `specs/FOR-158/` + `specs/FOR-161/` (in-flight training UI — coordinate).
- FOR-60 (states), FOR-61 (a11y), FOR-62 (theme). `AGENTS.md` — no domain logic in UI.
- Jira: https://dbhlab.atlassian.net/browse/FOR-167

## Repo Notes (verified)

- `frontend/src/pages/TrainingPage.tsx` + `trainingMuscleLabels.ts`; streak/weekly-history widgets under
  `frontend/src/pages/progress/` are reused on training per FOR-143.
- Completion actions + FOR-60 states present; keep them. Muscle-map grouping (FOR-160) is UI-only, untouched.

## Architectural Constraints

- Frontend-only, visual only; no progression/programming logic in UI (ADR-001); no change to data/actions.
- FOR-164 components + FOR-163 tokens; no hardcoded styling.
- Responsive + both themes + a11y preserved.

## Common Pitfalls

- Colliding with FOR-158 (breakdown) / FOR-161 (running-plan) mid-flight — coordinate merge order.
- Changing muscle-map grouping or streak logic while restyling.
- Dropping a completion action or FOR-60 state.
- Hardcoding visuals instead of tokens/components.

## Suggested Implementation Order

1. Confirm FOR-158/FOR-161 status; sequence accordingly.
2. Weekly calendar + session detail to the template via FOR-164 (+ tests).
3. Streak/weekly-history widgets + muscle-map framing restyle (+ tests), behaviour preserved.
4. Responsive + theme + a11y pass.

## Validation

Run frontend checks. Confirm the view matches the template; completion actions, streak, muscle-map and
FOR-60 states preserved; responsive/both-themes/a11y hold; no hardcoded visuals; no conflict with FOR-158/161.
