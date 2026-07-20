# FOR-160 AI Context

## Story

FOR-160 — Heatmap: agrupar variantes de hombro (extender normalización). Frontend-only.
**SHIPPED** in commit `96679be` (PR #150). This context documents the delivered change.

## Intent

Collapse FOR-154's new "hombro lateral" (and any sibling hombro variant) into the single canonical
"hombro" group in the training muscle map, so the heatmap does not show `hombro`, `hombro anterior`
and `hombro lateral` as three separate groups.

## Relevant Documents

- `specs/FOR-154/` — introduced the lateral-raise exercises (source of "hombro lateral").
- `specs/FOR-136/` — muscle-map read model.
- `specs/FOR-53/` — training screens (the note that flagged this).
- `AGENTS.md` — UI-only normalization; do not fabricate in the backend.
- Jira: https://dbhlab.atlassian.net/browse/FOR-160

## Repo Notes (verified — as shipped)

- `frontend/src/pages/trainingMuscleLabels.ts` — `MUSCLE_GROUPS` includes `['hombro lateral', 'hombro']`
  alongside `['hombro anterior', 'hombro']`; header comment documents which terms are genuine hombro
  synonyms vs deliberately-distinct.
- Merge rule (higher load wins) unchanged. A companion test covers the collapse.

## Architectural Constraints

- UI layer only; the backend muscle-map read model is not touched (repository priority).
- No domain logic in the frontend.

## Common Pitfalls (avoided)

- Grouping a term that is NOT a hombro synonym (kept deliberately distinct).
- Breaking the higher-load-wins merge rule when collapsing variants.
- Editing the backend read model instead of the UI normalization map.

## Validation

Frontend checks pass (`npm run test`, `typecheck`, `lint`, `build`); the muscle-map test asserts
hombro variants collapse to one group with the highest load. Delivered.
