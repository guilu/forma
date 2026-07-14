# FOR-119: Settings: working profile edit + units selector + training/nutrition prefs

Jira: https://dbhlab.atlassian.net/browse/FOR-119
Epic: FOR-47 UI & UX

## Summary

`ProfileSection.tsx` currently renders a disabled "Editar perfil" button
with a "Próximamente" badge and a static `MOCK_PROFILE` fixture (doc
comment: "there is no profile backend yet... every field here is read-only
display, never an editable form"). `UnitsSection.tsx` renders
non-interactive rows for the same reason ("no other screen currently reads
a shared unit preference to format against"). FOR-58's spec also flags
"Training / nutrition preference entry points" as folded into
`ObjectivesSection` with no distinct edit surface. FOR-107 (backend) now
exists — this story makes profile editing, the units selector, and
distinct training/nutrition preference entry points actually work against
it.

## User/System Flow

1. User opens Ajustes; `ProfileSection` loads the real profile from
   `GET /api/v1/profile` (FOR-107) instead of `MOCK_PROFILE`.
2. User clicks "Editar perfil" (now enabled); edits fields; saves; the
   backend persists and the section re-renders with the saved values +
   feedback (FOR-63).
3. User opens `UnitsSection`; it becomes a real selector; changing a unit
   persists via FOR-107 and (per FOR-107's scope) is available for other
   screens to read later.
4. User finds distinct entry points for training and nutrition preferences
   (previously folded into `ObjectivesSection`'s generic rows) that persist
   through FOR-107.

## Functional Requirements

- **Profile edit**: replace `MOCK_PROFILE` with data loaded from
  `GET /api/v1/profile`; enable "Editar perfil", remove the "Próximamente"
  badge/disabled state and the `edit-profile-hint` copy; build an edit form
  (name, email, birthDate, sex, heightCm, activityLevel, mainGoal) that
  saves via FOR-107's update endpoint; success/error feedback (FOR-63);
  loading/error states while the initial profile fetch is in flight
  (FOR-60).
- **Units selector**: replace `UnitsSection`'s static rows with an actual
  selector (or, if the MVP keeps a fixed metric set per FOR-107's Open
  Questions, at minimum reflect the persisted preference and allow
  changing it if FOR-107 exposes more than one option); persists via
  FOR-107.
- **Training/nutrition preference entry points**: give training and
  nutrition preferences their own visible entry point, distinct from
  `ObjectivesSection`'s default-objectives rows (FOR-58's spec documents
  this as a folded-in assumption that this story now resolves); persists
  through FOR-107.
- Loading/empty/error states for every new data fetch (FOR-60); a11y per
  FOR-61 (labelled fields, keyboard-operable, validation errors close to
  fields).

## Non-Functional Requirements

- No client-side validation logic duplicating backend rules beyond basic
  required-field/format checks (ADR-006) — the backend remains the source
  of truth for validity.
- Editable vs. read-only clearly distinguished, consistent with FOR-58's
  original rule ("never shows unsupported options as active") — now that a
  backend exists, these sections graduate from inert to real, but any
  field FOR-107 still doesn't support stays inert and documented.

## Data Model Notes

Consumes FOR-107's profile/preferences read + update endpoints. Removes
`MOCK_PROFILE`/`UNIT_PREFERENCES`/relevant parts of
`frontend/src/pages/settings/profileData.ts` in favor of real API calls
(keep `APP_VERSION`/`DEFAULT_OBJECTIVES` if they remain out of this
story's scope — `DEFAULT_OBJECTIVES` stays with `ObjectivesSection` unless
that section is also touched here for the training/nutrition entry
points).

## Edge Cases

- Profile fetch fails → `ErrorState` with retry (FOR-60), not a broken
  form.
- Save fails (validation error from FOR-107) → error shown close to the
  offending field, not a generic toast only (FOR-61).
- Concurrent edits (single-user MVP, unlikely but possible: two tabs) →
  last write wins, no corruption; not required to build conflict
  resolution for MVP.

## Open Questions

- Whether units become a real multi-option selector in this story or stay
  a single documented default reflecting FOR-107's persisted value — depends
  on FOR-107's final Open Questions resolution; document the outcome here
  during implementation.
