# FOR-143 AI Context

## Story

FOR-143 — Progreso: streak + weekly-history widgets (consume FOR-139). Frontend-only.
**SHIPPED** in commit `7163fcd` (PR #139). This context documents the delivered change.

## Intent

Replace FOR-53's static "RACHA ACTUAL" and weekly-history placeholders with real data from the
FOR-139 read models (`/progress/streak`, `/progress/weekly-history`).

## Relevant Documents

- `specs/FOR-139/` — streak + weekly-history read models and endpoints.
- `specs/FOR-53/` — training screens (the placeholders being replaced).
- `specs/FOR-124/` — the later insights history / WoW deltas work.
- `AGENTS.md` — frontend renders read models; no progression logic in UI.
- `docs/adr/ADR-001-domain-first.md`, `docs/3-entrenamiento.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-143

## Repo Notes (verified — as shipped)

- Progreso components under `frontend/src/pages/progress/` consume the FOR-139 endpoints.
- `GET /api/v1/progress/streak` → current + longest; `GET /api/v1/progress/weekly-history` → per-week series.
- FOR-60 states, `StatusPill`, `ChartContainer` reused; no new backend.

## Architectural Constraints

- Frontend-only; pure render of the read models (ADR-001).
- Streak signal is nutrition meal-log days (FOR-139 documented rule) — the UI does not re-derive it.
- Accessible states; no color-only meaning.

## Common Pitfalls (avoided)

- Treating an empty history as an error rather than a zeroed series.
- Implying a per-week training bar where no per-date training history exists.
- Re-deriving the streak in the UI.

## Validation

Frontend checks pass (`npm run test`, `typecheck`, `lint`, `build`); the widgets render current/longest
streak and the weekly series, with empty→zeroed handling. Delivered.
