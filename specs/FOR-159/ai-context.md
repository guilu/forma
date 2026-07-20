# FOR-159 AI Context

## Story

FOR-159 — UI de check-in semanal + DELETE de una semana (consume FOR-155). Frontend + a thin backend
DELETE slice.

## Intent

Give the user a real UI to record, edit and delete the weekly tracking rows (hoja _Seguimiento_),
and add the one missing backend operation (DELETE a week) so a mistaken week can be corrected.

## Relevant Documents

- `specs/FOR-155/` — weekly tracking read/write backend (GET/POST, fields, upsert semantics).
- `specs/FOR-150/` — insights rule 2 reads the weekly check-in (coherence with `BodyMeasurement`).
- `specs/FOR-52/` — body composition / measurements screens.
- `AGENTS.md` — hexagonal boundaries; frontend renders read models; no domain logic in UI.
- `docs/adr/ADR-001-domain-first.md`, `ADR-002`, `ADR-005-api-design.md`, `ADR-006-frontend.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-159

## Repo Notes (verified)

- No weekly-tracking UI exists (grep empty for `WeeklyTracking`/`pace4km` in `frontend/`).
- Backend: `delivery/tracking/WeeklyTrackingRecordController.java`, `WeeklyTrackingRecordService`,
  `WeeklyTrackingRecordRepository`, domain `WeeklyTrackingRecord` — GET/POST/{week} exist; **no DELETE**.
- Add DELETE by mirroring the existing controller→service→repository wiring; keep it thin (no policy).
- Frontend: new API client (`frontend/src/api/tracking.ts` or similar) + a new section under
  `frontend/src/pages/` (Progreso/Mediciones). Reuse FOR-60 states, `useNotify` (FOR-63), `Card` (FOR-112).

## Architectural Constraints

- Backend slice stays thin and owner-scoped (ADR-002); business rules live in the domain/service, not
  the controller (AGENTS.md).
- Frontend consumes endpoints; derived values (fat/lean mass) come from the backend, not the UI (ADR-001).
- Server-authoritative validation (ADR-006).
- Destructive delete requires explicit confirmation.

## Common Pitfalls

- Making DELETE succeed silently on a missing week (should be 404).
- Duplicating a week on edit instead of relying on POST-upsert.
- Recomputing derived mass/kcal in the UI.
- Mis-parsing `pace4kmMinPerKm` (mm:ss).
- Showing two contradictory body-fat numbers (measurement vs check-in) without the coherence decision.

## Suggested Implementation Order

1. Backend: `DELETE /api/v1/tracking/weekly/{week}` — repository delete, service, controller, 404 on
   missing (TDD: controller + service + repository tests first).
2. Frontend client: `deleteWeeklyTracking(week)` + the existing GET/POST client calls if not present (+ tests).
3. Frontend section: history list + add/edit form + delete-with-confirm, FOR-60 states, `useNotify` (+ tests).
4. Apply and document the body-fat coherence decision in the UI.

## Validation

Run backend checks (build + tests) and frontend checks (`npm run test`, `typecheck`, `lint`,
`format:check`, `build`). Confirm: DELETE removes a week and 404s on a missing one; add/edit upserts;
history renders (empty = normal); delete confirms + notifies; derived values come from the backend.
