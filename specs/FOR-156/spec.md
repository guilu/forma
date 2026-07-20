# FOR-156 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-156
Epic: FOR-47 UI & UX
Backend: FOR-149 (personal targets on the profile read model). Part of the frontend personalization batch (backend epic FOR-148).

## Summary

Show — and, where the section pattern supports it, edit — the profile's `personalTargets`
in Ajustes > `ObjectivesSection`. FOR-149 already exposes the targets on `GET /api/v1/profile`;
today `ObjectivesSection` only renders inert objectives from `profileData.ts`
(protein/water/deficit) and never reads these fields. Frontend-only.

## Repository baseline (verify before coding)

- `frontend/src/pages/settings/ObjectivesSection.tsx` — renders static objectives from
  `profileData.ts`; does NOT read `personalTargets`.
- `frontend/src/api/profile.ts` — profile client. Confirm the typed model already carries
  `personalTargets`; extend the type if it does not.
- `GET /api/v1/profile` (FOR-149) returns `personalTargets`:
  `baseCaloriesKcal` (e.g. 2300), `bodyFatTargetMinPct`/`bodyFatTargetMaxPct` (12–13),
  `weightTargetMinKg`/`weightTargetMaxKg` (73–75), `proteinTargetG` (160), `fatTargetG` (70),
  `carbsTargetG` (260). Confirm exact field names against the live payload / `specs/FOR-149`.

## User/System Flow

1. User opens Ajustes → Objetivos.
2. `ObjectivesSection` reads `personalTargets` from the profile read model.
3. Values render: base kcal, target body-fat range, target weight range, macro targets.
4. If the section supports editing, the user edits a target and saves → persisted via the
   profile update command; server-side validation (ADR-006) surfaces field errors.

## Functional Requirements

- Load `personalTargets` from the profile and render every field: `baseCaloriesKcal`,
  body-fat target range, weight target range, `proteinTargetG`, `fatTargetG`, `carbsTargetG`.
- Preserve the existing inert objectives only if they remain meaningful; otherwise replace
  them with the real targets (document the decision in the PR).
- Editing is in-scope ONLY if the section already has an edit pattern to follow; mirror it.
  Persist through the profile update endpoint; do not invent a new command shape.
- No business logic in the UI (ADR-001) — render backend-computed numbers; do not recompute
  or re-derive targets client-side.

## Non-Functional Requirements

- Loading / empty / error states via the FOR-60 shared components.
- Token-driven styling; no hardcoded visual rules.
- Validation is server-authoritative (ADR-006); the UI shows returned field errors close to
  the field.

## UI / States (see ui.md)

- Read-only render of the targets, plus edit affordance only where the section pattern allows.

## Edge Cases

- Profile has no `personalTargets` yet (older profile) → graceful empty/default display, not a
  crash; document whether the backend guarantees defaults.
- Partial targets (some fields null) → render only present fields.
- Save validation failure → field-level error, values not lost.

## Open Questions

- Does `ObjectivesSection` already support editing, or is it display-only today? If display-only,
  edit is deferred and this story is render-only (confirm in design).
- Keep or drop the legacy `profileData.ts` objectives once real targets render.
- Exact field names/units in the live `personalTargets` payload (confirm against FOR-149).
