# FOR-26 AI Context

## Story

FOR-26 — Create weekly training calendar
(https://dbhlab.atlassian.net/browse/FOR-26)

## Intent

Give the user a fast, mobile-friendly read on this week's training (runs,
strength, rest), replacing the Training placeholder. Success is a simple,
honest weekly view backed by real plan data when available.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`
- `docs/adr/ADR-006-frontend.md`
- `docs/adr/ADR-005-api-design.md` (the read model it consumes)
- `specs/FOR-22/`, `specs/FOR-23/`, `specs/FOR-25/` (the plan data it shows)
- `specs/FOR-18/`, `specs/FOR-19/` (frontend patterns: apiClient, states, Card)
- Jira: https://dbhlab.atlassian.net/browse/FOR-26

## Domain Notes

- The frontend renders read models (planned sessions + status); it owns no
  training rules (ADR-006).
- Statuses shown here are planned/completed; changing them is FOR-27.

## Architectural Constraints

- Replace `frontend/src/pages/TrainingPage.tsx` (currently a `PagePlaceholder`);
  routing at `/entrenamiento` is already wired (`frontend/src/app/routes.tsx`).
- Call the API only through `frontend/src/api/client.ts` (relative `/api/...`,
  same-origin — see the FOR-18/FOR-19 body API modules for the pattern).
- Reuse `Card` and design tokens; add loading/empty/error states.

## Common Pitfalls

- Assuming a Training API already exists — it does not (only Body endpoints are
  implemented). Resolve the data source explicitly (spec.md Open Questions).
- Bypassing `apiClient` with direct `fetch`.
- Duplicating plan/summary logic client-side.
- Skipping mobile layout or the empty/error states.

## Suggested Implementation Order

1. Resolve the weekly-plan data source (endpoint or interim) and document it.
2. Build the week view (7 days; running/strength/rest entries) with `Card`.
3. Wire data via `apiClient`; add loading/empty/error states.
4. Verify mobile readability against docs/ui-guidelines.md.

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`, AGENTS.md "Frontend" row).
