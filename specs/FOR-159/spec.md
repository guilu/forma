# FOR-159 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-159
Epic: FOR-47 UI & UX
Backend: FOR-155 (weekly tracking read/write). Frontend personalization batch. **Includes a thin backend vertical slice** (a new DELETE endpoint).

## Summary

UI to enter, edit and delete the weekly tracking rows (hoja _Seguimiento_), consuming FOR-155, plus
a small backend slice: a `DELETE /api/v1/tracking/weekly/{week}` so a week can be corrected/removed.
No weekly-tracking UI exists today. Frontend + a minimal backend endpoint.

## Repository baseline (verified)

- No weekly-tracking UI exists (grep empty for `tracking/weekly` / `WeeklyTracking` / `pace4km` in `frontend/`).
- Backend FOR-155 already provides `GET/POST /api/v1/tracking/weekly` and `GET .../{week}` via
  `delivery/tracking/WeeklyTrackingRecordController.java` (+ service + `WeeklyTrackingRecordRepository`),
  with fields: week, date, weight, body-fat %, BMI, running km, `pace4kmMinPerKm`, recommended kcal,
  comment (fat/lean mass derived). POST is an upsert per week.
- **DELETE is missing** — POST upserts by week, so a week cannot currently be removed.

## Backend slice (this story)

- Add `DELETE /api/v1/tracking/weekly/{week}` — owner-scoped (ADR-002), 404 if the week does not
  exist, idempotent success on delete. Thin controller → service → repository, mirroring the
  existing GET/POST wiring. See `api.md`.

## Coherence decision (resolve here)

Body-fat is captured both in `BodyMeasurement` (per-event, MeasurementsPage) and in the weekly
check-in (`WeeklyTrackingRecord`). Insights (FOR-150 rule 2) read the check-in; the older body rule
reads `BodyMeasurement` → they can contradict. Decide and document: which source is authoritative,
and make the UI state clearly what feeds what (do not silently show two conflicting body-fat numbers).

## User/System Flow

1. User opens the weekly tracking section (Progreso/Mediciones).
2. History list renders existing weeks (`GET /tracking/weekly`); empty is a normal starting state.
3. User adds a week via a form (`POST` — upsert); editing a week re-POSTs the same week.
4. User deletes a week (`DELETE /tracking/weekly/{week}`) with a confirmation.

## Functional Requirements

- Add/edit form for one weekly row: week, date, weight, body-fat %, BMI, running km,
  `pace4kmMinPerKm` (mm:ss), recommended kcal, comment. Edit = re-POST (upsert).
- History listing of past weeks.
- Delete a week via the new DELETE endpoint, with confirmation and feedback.
- No domain logic in the UI (ADR-001) — derived values (fat/lean mass) come from the backend.
- Server-authoritative validation (ADR-006).

## Non-Functional Requirements

- Loading / empty / error states (FOR-60); empty history is normal, not an error.
- `useNotify` (FOR-63) for save/delete feedback; destructive delete requires explicit confirmation.
- Backend endpoint owner-scoped; no cross-owner access.

## Edge Cases

- Empty history (fresh user) → `EmptyState`, not an error.
- Editing an existing week → upsert overwrites, no duplicate row.
- Delete a non-existent week → backend 404 surfaced as a readable message.
- `pace4kmMinPerKm` parsing/format (mm:ss) — validate format; server rejects bad values.
- Conflicting body-fat between measurement and check-in → resolved per the coherence decision above.

## Open Questions

- Where the section lives (Progreso vs Mediciones) — confirm against the mockups.
- The authoritative body-fat source and how the UI communicates it (coherence decision).
- Whether DELETE is hard-delete or soft — default hard-delete, owner-scoped, documented.
