# FOR-160 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-160 (Jira status "Por hacer")
Epic: FOR-47 UI & UX
Backend: FOR-136 / FOR-154. Note from FOR-53. Frontend personalization batch.

## Status: SHIPPED (repo has priority over Jira)

Implemented and merged in commit `96679be` — _"feat(training): group hombro lateral into hombro
muscle map (FOR-160)" (PR #150)_. Jira still reads "Por hacer"; the code is the source of truth. This
spec documents the delivered behaviour.

## Summary

Extend the heatmap muscle normalization to group the "hombro" variants introduced by FOR-154. The
`MUSCLE_GROUPS` map already collapsed `"hombro anterior" → "hombro"`; FOR-154's new exercises (lateral
raises) added `"hombro lateral"`, which was rendering as a separate group. This story added
`"hombro lateral" → "hombro"` so the Martes/Empuje muscle map shows a single "hombro" group.
Frontend-only (UI layer).

## Repository baseline (verified — as shipped)

- `frontend/src/pages/trainingMuscleLabels.ts` — `MUSCLE_GROUPS` now contains both
  `['hombro anterior', 'hombro']` and `['hombro lateral', 'hombro']` (verified in the repo).
- The existing merge rule is preserved: when multiple raw muscles collapse to one group, the higher
  load wins.
- Backend read model untouched (repository priority; nothing fabricated in the backend).

## Functional Requirements (delivered)

- `MUSCLE_GROUPS` maps `"hombro lateral" → "hombro"` (and any other FOR-154 hombro variant surfaced by
  the real muscle-map output).
- Merge semantics unchanged: highest load wins on collapse.
- UI layer only — the backend muscle-map read model is not modified.

## Non-Functional Requirements

- Pure UI normalization; no backend change, no domain logic.
- Token-driven styling unchanged.

## Edge Cases (covered)

- Multiple hombro variants in one session collapse to a single "hombro" group with the highest load.
- A genuinely distinct catalog term (not a hombro synonym) is deliberately left ungrouped (documented
  in the file's header comment).

## Open Questions

- None outstanding — shipped. If FOR-154 later adds further hombro variants, extend `MUSCLE_GROUPS`
  the same way.
