# FOR-143 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-143 (Jira status "Listo")
Epic: FOR-47 UI & UX
Backend: FOR-139. Screens: FOR-53. Frontend personalization batch.

## Status: SHIPPED (repo has priority over Jira)

Implemented and merged in commit `7163fcd` — _"feat(progress): wire streak + weekly-history widgets
(FOR-143)" (PR #139)_. This spec documents the delivered behaviour retroactively.

## Summary

Wire the FOR-139 backend into the Progreso/Entrenamiento UI: the "RACHA ACTUAL" streak indicator and
the weekly-history bars, which FOR-53 had shipped as static placeholders (no endpoint backed them).
FOR-139 added `GET /api/v1/progress/streak` (current + longest) and
`GET /api/v1/progress/weekly-history` (per-week series). Frontend-only — pure render of the read models.

## Repository baseline (verified — as shipped)

- FOR-53 rendered "RACHA ACTUAL" and the weekly-history bars as placeholders.
- FOR-139 provides `GET /api/v1/progress/streak` and `GET /api/v1/progress/weekly-history` (mounted on
  `ProgressController`).
- The Progreso section components live under `frontend/src/pages/progress/`.

## Functional Requirements (delivered)

- Streak widget consumes `GET /api/v1/progress/streak` (current + longest streak) with FOR-60
  loading/empty/error states.
- Weekly-history bars consume `GET /api/v1/progress/weekly-history` (per-week series).
- No progression rules in the UI (ADR-001) — pure render of the read models.
- Reuse of existing components (`StatusPill`, `ChartContainer`, etc.); no new backend.

## Non-Functional Requirements

- FOR-60 states; accessible rendering (text, not color alone).
- Token-driven styling.

## Edge Cases (covered)

- Empty history → streak 0/0 and zeroed weekly buckets (series still present), never an error.
- The streak signal = nutrition meal-log days (documented FOR-139 rule); there is no per-date
  training-completion history, so no per-week training bar is implied — the UI surfaces only what the
  endpoints return.

## Open Questions

- None outstanding — shipped. Historical/WoW extensions were carried by FOR-124.
