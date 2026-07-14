# FOR-119 AI Context

## Story

FOR-119 — Settings: working profile edit + units selector + training/
nutrition prefs (https://dbhlab.atlassian.net/browse/FOR-119)

## Intent

Three related FOR-58 deferrals ("Editar perfil" disabled, units read-only,
training/nutrition prefs folded into `ObjectivesSection`) all trace back to
the same root cause: no profile/preferences backend existed. FOR-107 closes
that gap; this story is the frontend graduation from "documented mock" to
"real, working settings."

## Blocked by

FOR-107 (backend: profile, unit preferences, default objectives, training/
nutrition preference persistence). Do not start until FOR-107's endpoints
exist.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (validation errors close to fields, no
  unsupported flows shown as active)
- `specs/FOR-107/spec.md` (the API surface this story consumes)
- `specs/FOR-58/spec.md`, `specs/FOR-58/ui.md` (original Ajustes story;
  read the Data Model Notes and Open Questions — this story resolves both)
- `specs/FOR-60/`, `specs/FOR-63/`, `specs/FOR-61/` (loading/error states,
  feedback, accessibility patterns to reuse)
- Jira: https://dbhlab.atlassian.net/browse/FOR-119

## Domain Notes

- `frontend/src/pages/settings/ProfileSection.tsx` — read the full doc
  comment; it explicitly names every deferral this story resolves
  ("Editar perfil... stays a disabled Button", "no profile backend yet").
- `frontend/src/pages/settings/UnitsSection.tsx` — doc comment explains
  exactly why it's read-only today ("no other screen currently reads a
  shared unit preference to format against").
- `frontend/src/pages/settings/ObjectivesSection.tsx` — doc comment notes
  it's "also covers the spec's separate 'Training / nutrition preference
  entry points' bullet... documented assumption" — this story is expected
  to give those their own entry point instead.
- `frontend/src/pages/settings/profileData.ts` — holds `MOCK_PROFILE`,
  `UNIT_PREFERENCES`, `DEFAULT_OBJECTIVES`, `APP_VERSION`; only the
  profile/units mocks are in scope for replacement here.

## Architectural Constraints

- Frontend performs only display-level validation (required fields,
  format) — authoritative validation stays server-side (ADR-005/ADR-006).
- Reuse existing feedback/state patterns (`useNotify`, `LoadingState`,
  `ErrorState`, `SavedIndicator`) rather than inventing new ones.

## Common Pitfalls

- Leaving the "Próximamente" badge/disabled state in place after wiring
  real data — the whole point of this story is removing it.
- Duplicating backend validation rules client-side instead of surfacing
  the backend's actual error response.
- Conflating the units selector with training/nutrition prefs — FOR-58's
  spec treats them as a documented, temporary conflation this story is
  meant to undo, not preserve.

## Suggested Implementation Order

1. Confirm FOR-107 has shipped; wire `ProfileSection` to real `GET`/update
   endpoints, remove `MOCK_PROFILE`.
2. Build the profile edit form + save flow + feedback.
3. Wire `UnitsSection` to the persisted preference (selector or display,
   per the Open Question).
4. Add the distinct training/nutrition preference entry point.
5. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise profile edit, units and the new preference entry
points against a local/dev backend once FOR-107 is available.
