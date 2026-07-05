# FOR-18: Create body measurement form (frontend)

Jira: https://dbhlab.atlassian.net/browse/FOR-18
Epic: FOR-2 Body Composition

## Summary

Replace the current placeholder `MeasurementsPage`
(`frontend/src/pages/MeasurementsPage.tsx`) with a real form to enter a body
measurement, calling the FOR-17 API. Validates required fields, shows clear
success/error states, and works on mobile. No auth yet.

## User/System Flow

1. User opens the Measurements page (already routed, currently a
   `PagePlaceholder`).
2. User fills in date+time, weight, body fat %, BMI, and optional notes.
3. User submits; the frontend calls `POST /api/v1/body/measurements` (FOR-17)
   through the shared API client (`frontend/src/api/client.ts`).
4. On success, the form clears/confirms and any measurement list/dashboard
   state refreshes. On error, the API error message is shown near the form.

## Functional Requirements

- Form fields: measurement date+time, `weightKg`, `bodyFatPercentage`,
  `bmi`, optional `notes` — matching the FOR-17 `POST` request contract
  (`specs/FOR-17/api.md`).
- Client-side validation for required fields before submit (measurement
  date+time, weight, body fat %, BMI), in addition to the server-side
  validation FOR-17 already enforces.
- Use the shared `apiClient` (`frontend/src/api/client.ts`) rather than a
  direct `fetch` call, per its existing boundary comment and ADR-006.
- On success, refresh whatever local/measurement list state the page holds
  (and, if already present, dashboard state) so the new measurement is
  visible without a manual reload.
- On error, surface the API's error message (`ApiError.message`) without
  exposing raw response bodies/stack traces.
- Do not duplicate the FOR-15 derived-value calculation in the frontend
  (fat mass/lean mass are read from the API response only, if displayed
  here at all) — per ADR-006 "do not duplicate backend calculations".

## Non-Functional Requirements

- Usable on mobile: docs/ui-guidelines.md lists "Add measurement" as mobile
  priority #2.
- Accessible labels and focus states for all fields (ADR-006).
- No auth/authorization handling in this story (single-user MVP).

## Data Model Notes

Form fields and submitted payload mirror `specs/FOR-17/api.md`'s `POST`
request body exactly; no additional fields are introduced.

## Edge Cases

- Submit with a required field empty — block submit, show inline validation
  errors near the fields (ADR-006: "Forms must display validation errors
  close to fields").
- API returns a validation error (400) after client-side checks pass (e.g. a
  rule only enforced server-side) — show the server message, do not silently
  ignore it.
- Network/API failure (5xx or unreachable) — show a generic error state, not
  a stack trace.

## Open Questions

- Whether the form also displays computed `fatMassKg`/`leanMassKg` after
  submit, or defers that entirely to FOR-19's dashboard cards — Jira does not
  specify; recommend deferring to FOR-19 to keep this story focused on entry
  only.
