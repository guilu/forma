# FOR-161 Test Plan

Strict TDD: failing tests first — both the backend endpoint and the frontend view.

## Backend (running-plan endpoint)

- Controller (`TrainingControllerTest`): `GET /api/v1/training/running-plan` → 200 with the full plan;
  assert it returns the expected number of weeks (16) and that deload weeks carry the explicit marker.
- The endpoint delegates to `RunningPlanGenerator` and maps to a DTO — assert distances/km/week match
  the generator output (no recomputation in the controller).
- The domain object is not leaked (DTO shape asserted).

## Frontend API client (`training.ts`)

- `getRunningPlan()` → `GET /api/v1/training/running-plan`, returns the typed `weeks` array.
- Error surfaces as a rejected promise.

## Frontend plan view

- Renders all weeks returned (do not assume 16 — assert it renders `weeks.length` rows).
- Deload weeks are visually + textually distinguished (assert a text/marker, not just a class).
- Each week shows its sessions, distances and weekly volume.
- Loading → `LoadingState`; error → `ErrorState`; empty (unlikely) → `EmptyState` (FOR-60).

## Accessibility

- Plan is a semantic table/list; deload conveyed with text, not color alone.
- States announced (`role="status"` / `role="alert"`); axe coverage extended.

## Fixtures

- Backend: the real generator (or a fixture plan) asserting week count + deload cadence.
- Frontend: a mocked `running-plan` payload with progression + deload weeks, plus an error case.
