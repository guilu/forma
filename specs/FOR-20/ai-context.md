# FOR-20 AI Context

## Story

FOR-20 — Create body progress graphs
(https://dbhlab.atlassian.net/browse/FOR-20)

## Intent

Give the user a simple visual read on weight, body fat % and lean mass trends
using real FOR-17 API data, replacing the current Progress placeholder.
Success is honest, readable graphs for recent data with a real empty state —
not a feature-complete charting system.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md`
- `docs/adr/ADR-006-frontend.md`
- `specs/FOR-17/` (API this page reads)
- Jira: https://dbhlab.atlassian.net/browse/FOR-20

## Domain Notes

Series values (`weightKg`, `bodyFatPercentage`, `leanMassKg`) come straight
from `BodyMeasurement` API responses; nothing new is derived on the
frontend.

## Architectural Constraints

- Replace `frontend/src/pages/ProgressPage.tsx` (currently a
  `PagePlaceholder`).
- Call the API only through `frontend/src/api/client.ts`.
- **No chart library is currently a dependency** of `frontend/package.json`
  — this must be resolved before/while implementing (see spec.md Open
  Questions). Do not assume a specific library exists; verify
  `frontend/package.json` before writing code that imports one.

## Common Pitfalls

- Assuming a chart library (e.g. Recharts) is already installed — it is
  not, as of this spec.
- Building a full-featured charting system (zoom, filters, multi-range
  comparison) — explicitly out of scope for this MVP slice
  ("avoid complex filters in MVP").
- Faking data when the API returns few/zero points instead of showing an
  honest empty or single-point state.
- Duplicating derived-value math instead of reading `leanMassKg` from the
  API.

## Suggested Implementation Order

1. Resolve the charting approach (pick/add a lightweight library, or use
   plain SVG) and record the decision.
2. Fetch measurements via `apiClient` and shape series for the three
   metrics.
3. Render the weight and body fat % graphs; render or prepare the lean mass
   series.
4. Implement the empty/sparse-data states.
5. Verify mobile readability against docs/ui-guidelines.md ("avoid noisy
   charts").

## Validation

Run the frontend build and tests (`npm run build`, `npm run test` in
`frontend/`, per AGENTS.md Verification guidance "Frontend" row).
