# FOR-26: Create weekly training calendar

Jira: https://dbhlab.atlassian.net/browse/FOR-26
Epic: FOR-3 Training Engine

## Summary

Replace the placeholder `TrainingPage`
(`frontend/src/pages/TrainingPage.tsx`) with a weekly training calendar that
combines planned running sessions (FOR-22/FOR-23), planned strength sessions
(FOR-25) and rest days. Simple, mobile-friendly; each entry shows session type,
distance or workout name, and status (planned/completed).

## User/System Flow

1. User opens the Training page (already routed at `/entrenamiento`, currently a
   `PagePlaceholder`).
2. Frontend loads the current week's planned sessions (running + strength) and
   renders a day-by-day view; days without a session show as rest.
3. Each session shows its type, distance (running) or workout name (strength),
   and status. Completion state (planned/completed) is displayed; changing it is
   FOR-27.

## Functional Requirements

- Weekly calendar view exists on the Training page, using existing UI
  primitives (`Card`, design tokens) rather than a new styling approach.
- Running sessions appear on their planned days; strength sessions appear on
  their planned days; rest days are clearly visible.
- Each entry shows: session type, distance-or-workout-name, and status
  (planned/completed).
- Data comes from the real plan when available; the first version may use
  seeded/generated data (FOR-23/FOR-25). Call the backend only through the
  shared `apiClient` (`frontend/src/api/client.ts`, relative `/api/...`), never
  ad-hoc `fetch`.
- Works on mobile (docs/ui-guidelines.md: keep it simple; avoid noisy UI).

## Non-Functional Requirements

- No domain logic duplicated in the frontend (ADR-006); the frontend renders
  plan/status read models from the API.
- Loading, empty and error states handled (ADR-006) for the week's data.

## Data Model Notes

The calendar consumes a week of planned sessions (running + strength) with
status. **Repo gap**: no Training API/read model exists yet (only Body endpoints
are implemented, `docs/api/body-measurements.md`). Serving the weekly plan needs
either a new backend endpoint or a documented interim data source — resolve
during implementation (see Open Questions); do not invent an unspecified
contract.

## Edge Cases

- A week with no planned sessions — empty state, not a broken grid.
- A day with both a run and a strength session — both shown for that day.
- Data/API load failure — error state (ADR-006), not a crash.

## Open Questions

- **Data source**: which endpoint feeds the week? No Training API exists yet.
  Options: add a read endpoint for the weekly plan (separate/explicit backend
  work), or read seeded data via an interim mechanism. Document the chosen
  approach; keep the frontend calling `apiClient` with a relative path.
- Which week is shown by default (current calendar week vs. plan week 1) and
  whether week navigation is in scope — recommend current week only, no
  navigation, for this MVP slice.
