# FOR-159 Test Plan

Strict TDD: failing tests first, then implement — both the backend slice and the frontend.

## Backend (DELETE slice)

- Repository: deleting an existing week removes it (owner-scoped); deleting a missing week is
  reported as not-found to the service.
- Service: `delete(week)` for a present week succeeds; for an absent week raises the not-found path.
- Controller (`WeeklyTrackingRecordControllerTest`): `DELETE /api/v1/tracking/weekly/{week}` →
  204/empty on success; missing week → 404 `NOT_FOUND`; malformed `week` → 400 `VALIDATION_ERROR`.
- Owner-scoping: a delete never affects another owner's row (as far as the single-user posture allows).

## Frontend API client (`tracking.ts`)

- `listWeeklyTracking()` → `GET /api/v1/tracking/weekly`.
- `getWeeklyTracking(week)` → `GET .../{week}`.
- `upsertWeeklyTracking(payload)` → `POST /api/v1/tracking/weekly`.
- `deleteWeeklyTracking(week)` → `DELETE .../{week}`; 404 surfaces as a rejected promise.

## Frontend section

- Empty history → `EmptyState` (normal starting state, not an error).
- History list renders existing weeks.
- Add form submits a new week (POST); edit re-POSTs the same week (upsert, no duplicate row).
- Delete requires confirmation; on confirm calls `deleteWeeklyTracking` and shows a success toast.
- Delete of a missing week (404) → readable error, list unchanged.
- `pace4kmMinPerKm` shown/entered as mm:ss; invalid format rejected (server-side) with a field error.
- Loading → `LoadingState`; fetch error → `ErrorState` (FOR-60).

## Accessibility

- Form fields labelled; validation errors associated + announced.
- Destructive confirm is keyboard-operable; states announced (`role="status"`/`role="alert"`); axe extended.

## Fixtures

- Mocked tracking client (list/get/upsert/delete + a 404 delete + a validation error).
- Backend test fixtures for present/absent week.
