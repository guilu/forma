# FOR-34 AI Context

## Story

FOR-34 — Create late running day meal flow
(https://dbhlab.atlassian.net/browse/FOR-34)

## Intent

Make the running-day plan practical for late-evening summer runs: carbs earlier,
light dinner, optional post-run protein — and explain it in the UI. Success is a
clear, ordered running-day flow shown on the Nutrition page, backed by the FOR-33
template, that still hits macro targets.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` ("Late running nutrition UX", interaction style)
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-005-api-design.md`
- `specs/FOR-29/`, `specs/FOR-31/`, `specs/FOR-32/`, `specs/FOR-33/`
- `specs/FOR-18/`, `specs/FOR-26/` (frontend patterns: apiClient, states, Card;
  and the "no API yet" data-source gap precedent)
- Jira: https://dbhlab.atlassian.net/browse/FOR-34

## Domain Notes

- Template behaviour, not a strict rule: the running day orders meals so carbs
  land earlier and dinner is light; post-run protein is optional.
- The frontend renders read models; it owns no nutrition rules and never
  recomputes macros (ADR-006).

## Architectural Constraints

- Replace/extend `frontend/src/pages/NutritionPage.tsx` (currently a
  `PagePlaceholder`); routing at `/nutricion` is already wired.
- Call the API only through `frontend/src/api/client.ts` (relative `/api/...`)
  if a data source exists (see spec Open Questions).
- Reuse `Card` and design tokens; handle loading/empty/error states.

## Common Pitfalls

- Assuming a Nutrition API already exists — it does not (like the FOR-26 gap).
  Resolve the data source explicitly.
- Duplicating macro calc in the component (use FOR-32 via the read model).
- Forcing post-run whey when protein is already met.
- Prescriptive/gamified copy.

## Suggested Implementation Order

1. Confirm/shape the RUNNING template flow in FOR-33 (pre-run snack, optional
   post-run, light dinner) and its macro totals.
2. Resolve the Nutrition data source (endpoint or interim) and document it.
3. Build the Nutrition page flow (ordered meals + explanation) with `Card`.
4. Add loading/empty/error states; verify mobile readability and neutral copy.

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`); backend tests if the running template/data source is touched.
