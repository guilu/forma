# FOR-160 Test Plan

**SHIPPED** — tests delivered with commit `96679be`. Documented here for completeness.

## Scope

The UI muscle-label normalization in `frontend/src/pages/trainingMuscleLabels.ts`. No backend.

## Cases (delivered)

- `"hombro lateral"` normalizes to `"hombro"` (new mapping).
- `"hombro anterior"` still normalizes to `"hombro"` (existing mapping preserved).
- Multiple hombro variants in one session collapse to a single `"hombro"` group whose load is the
  **highest** of the merged raw muscles (merge rule preserved).
- A deliberately-distinct catalog term (not a hombro synonym) is NOT grouped into hombro.

## Fixtures

- Muscle-map inputs containing `hombro`, `hombro anterior` and `hombro lateral` with differing loads.
