# FOR-161 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-161
Epic: FOR-47 UI & UX
Backend: FOR-153 (running plan domain: `RunningPlanGenerator`). Frontend personalization batch. **Includes a thin backend vertical slice** (a new GET endpoint).

## Summary

Show the full 16-week running plan (progression + deloads), not just the current week. Today the UI
only sees the current week via `GET /api/v1/training/week`. The 16-week plan (FOR-153: volume
13→19 km, 6×400m, deload every 4 weeks) is internal domain logic (`RunningPlanGenerator`) **not
exposed by any API**. This story adds a `GET /api/v1/training/running-plan` endpoint over the
generator and a frontend view. Backend slice + frontend.

## Repository baseline (verified)

- `TrainingController.java` (`delivery/training/`) exposes only `/week`, `/weekly-summary`,
  `/sessions/{id}/muscle-map` — **no running-plan endpoint**.
- `domain/RunningPlanGenerator.java` already produces the 16-week plan (progression, 6×400m, deload
  every 4 weeks) but nothing surfaces it.
- Frontend: `frontend/src/pages/TrainingPage.tsx` shows the current week only; `frontend/src/api/training.ts`
  has no running-plan client.

## Backend slice (this story)

- Add `GET /api/v1/training/running-plan` exposing the 16 weeks from `RunningPlanGenerator`: per week
  the 3 sessions, distances, km/week, and a deload marker. Owner-scoped reference data (ADR-002), thin
  controller + a response DTO over the generator. See `api.md`.

## User/System Flow

1. User opens Entrenamiento → running plan view.
2. Frontend GETs `/api/v1/training/running-plan`.
3. The 16 weeks render as a table/list, deload weeks highlighted.

## Functional Requirements

- Backend: `GET /api/v1/training/running-plan` returns the 16-week plan from `RunningPlanGenerator`
  (no new domain logic — expose what the generator computes).
- Frontend: a plan view (table/list of 16 weeks) in Entrenamiento, highlighting deload weeks; each
  week shows its sessions, distances and weekly volume.
- No progression logic in the UI (ADR-001) — render the read model as returned.

## Non-Functional Requirements

- Loading / empty / error states (FOR-60).
- Backend endpoint owner-scoped reference data (ADR-002); response is a DTO, not the domain object.
- Token-driven styling; reuse existing training components.

## Edge Cases

- Deload week rendering distinct from progression weeks (text + marker, not color alone).
- Endpoint error → `ErrorState`; the rest of the training page still works.
- Plan shape/length change (if the generator is parameterised later) → the view renders whatever
  weeks the endpoint returns (do not hardcode "16").

## Open Questions

- Exact DTO shape for a week (sessions + distances + km/week + deload flag) — finalise in design
  against `RunningPlanGenerator`'s output and `docs/3-entrenamiento.png`.
- Whether the plan view is a dedicated route/section or a panel within `TrainingPage`.
- Whether the current week is highlighted within the 16-week view (cross-reference with `/week`).
