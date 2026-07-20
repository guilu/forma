# FOR-160 UI Spec

**SHIPPED** in commit `96679be`. Documents the delivered UI behaviour.

## Screens

- Entrenamiento → session muscle map / heatmap (`frontend/src/pages/TrainingPage.tsx` consuming
  `trainingMuscleLabels.ts`; mockup `docs/3-entrenamiento.png`).

## Components

- `trainingMuscleLabels.ts` `MUSCLE_GROUPS` normalization map (the changed unit).
- The muscle-map/heatmap rendering component (unchanged; it consumes the normalized labels).

## Behaviour

- Hombro variants (`hombro`, `hombro anterior`, `hombro lateral`) render as a single "Hombro" group.
- The group's displayed load is the highest among the merged raw muscles (merge rule unchanged).
- Canonical keys are capitalized for display (e.g. "hombro" → "Hombro").

## Accessibility

- No change — the grouping only reduces duplicate labels; contrast/labels of the heatmap are unaffected.

## Responsive Behavior

- Unchanged; the muscle map follows the existing training-page responsive pattern.
