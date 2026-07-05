# FOR-18 AI Context

## Story

FOR-18 — Create body measurement form (frontend)
(https://dbhlab.atlassian.net/browse/FOR-18)

## Intent

Give the user a way to record a manual measurement through the UI, backed by
the FOR-17 API. Success is a working form on the existing Measurements route
that validates input, shows clear feedback, and works on mobile — without
reimplementing backend logic.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`
- `docs/adr/ADR-006-frontend.md`
- `specs/FOR-17/` (API contract this form calls)
- Jira: https://dbhlab.atlassian.net/browse/FOR-18

## Domain Notes

The frontend must not recompute `fatMassKg`/`leanMassKg` — those are backend
derived values (FOR-15). If shown at all in this story, they must come from
the API response.

## Architectural Constraints

- Replace `frontend/src/pages/MeasurementsPage.tsx` (currently a
  `PagePlaceholder`, see `frontend/src/components/PagePlaceholder.tsx`).
- Route to this page and navigation are already wired
  (`frontend/src/app/routes.tsx`, `frontend/src/app/navigation.ts`) — no
  routing changes expected.
- Call the API only through `frontend/src/api/client.ts`'s `apiClient`, not a
  new ad-hoc `fetch`.
- Reuse existing UI primitives (e.g. `frontend/src/components/Card.tsx`)
  rather than introducing a parallel styling approach.

## Common Pitfalls

- Bypassing `apiClient` and calling `fetch` directly.
- Recomputing derived body-composition values in the component.
- Skipping mobile layout — docs/ui-guidelines.md explicitly prioritizes
  "Add measurement" on mobile.
- Missing inline validation messages (ADR-006 requires errors close to
  fields, not a single top-level banner only).

## Suggested Implementation Order

1. Build the form fields and client-side validation.
2. Wire submit to `apiClient.request` against
   `POST /api/v1/body/measurements`.
3. Implement loading/success/error states.
4. Confirm mobile layout against docs/ui-guidelines.md mobile considerations.
5. Wire success to refresh list/dashboard state per spec.md.

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`, per AGENTS.md Verification guidance "Frontend" row — the Vite/
Vitest frontend baseline already exists in the repo).
