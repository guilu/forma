# FOR-161 AI Context

## Story

FOR-161 — Vista del plan de running de 16 semanas (endpoint + UI). Backend GET slice over
`RunningPlanGenerator` + a frontend plan view.

## Intent

Expose the full 16-week running plan the domain already generates (progression, 6×400m, deload every
4 weeks) and render it, so the user sees the whole block — not just the current week.

## Relevant Documents

- `specs/FOR-153/` — running plan domain (volume 13→19 km, 6×400m, deload cadence).
- `specs/FOR-53/` — training screens.
- `AGENTS.md` — hexagonal boundaries; no business rules in delivery; frontend renders read models.
- `docs/adr/ADR-001-domain-first.md`, `ADR-002`, `ADR-005-api-design.md`, `docs/3-entrenamiento.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-161

## Repo Notes (verified)

- `backend/.../delivery/training/TrainingController.java` — only `/week`, `/weekly-summary`,
  `/sessions/{id}/muscle-map`; add `/running-plan` here (thin).
- `backend/.../domain/RunningPlanGenerator.java` — already computes the 16-week plan; expose it via a DTO.
- `frontend/src/pages/TrainingPage.tsx` — current week only; add the plan view.
- `frontend/src/api/training.ts` — add a `getRunningPlan()` client.
- Reuse FOR-60 states, `Card`/`headingLevel` (FOR-112), existing training components.

## Architectural Constraints

- Backend slice is thin: controller → `RunningPlanGenerator` → DTO. **No progression/deload logic in
  the controller** (AGENTS.md); reuse the generator.
- Response is a DTO, not the domain object.
- Frontend renders the plan as returned — no progression logic in the UI (ADR-001).
- Owner-scoped reference data (ADR-002).

## Common Pitfalls

- Re-implementing the deload/progression rule in the controller or the UI instead of reading the
  generator's `deload` marker / distances.
- Serialising the domain object directly instead of a DTO.
- Hardcoding "16 weeks" in the UI rather than rendering whatever weeks the endpoint returns.
- Deload highlighted by color only (needs a text/marker too).

## Suggested Implementation Order

1. Backend: response DTO over `RunningPlanGenerator` + `GET /api/v1/training/running-plan` on
   `TrainingController` (TDD: controller test asserting 16 weeks + deload markers first).
2. Frontend client: `getRunningPlan()` + response type in `training.ts` (+ client test).
3. Frontend plan view (table/list of 16 weeks, deloads highlighted, FOR-60 states) in Entrenamiento (+ tests).

## Validation

Run backend checks (build + tests) and frontend checks (`npm run test`, `typecheck`, `lint`,
`format:check`, `build`). Confirm the endpoint returns the full plan with deload markers, the view
renders all weeks and highlights deloads (text + marker), and errors degrade gracefully. No plan
logic duplicated in delivery or UI.
