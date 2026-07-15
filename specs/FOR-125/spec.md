# FOR-125 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-125
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-104 [STUB] Progress & goals domain (slice 1 of 6). Blocks FOR-122.

## Summary

First implementable slice of FOR-104: the **Goals + milestones** backend. A `Goal`
aggregate with `Milestone`s, persistence, and read/create/update HTTP endpoints, with
goal progress derived from real source data where a link exists. This slice
deliberately excludes adherence, streaks, achievements, progress photos and the
muscle-worked map — those are later FOR-104 slices. See `specs/FOR-104/` for the full
domain scope and open questions.

## User/System Flow

1. User opens Objetivos (FOR-122) → `GET /api/v1/goals` returns goals with progress + milestones.
2. User creates a goal with milestones → `POST /api/v1/goals` persists it.
3. User edits a goal or marks a milestone → `PATCH /api/v1/goals/{id}`.
4. Progress for each goal is derived (not stored) from linked source data where a link exists.

## Functional Requirements

- Persist a `Goal` aggregate: title, metric, target, optional due date, and an ordered list of `Milestone`s (title + target, completion state).
- Create / read / update goals over HTTP (see `api.md`).
- Derive goal progress from linked real data (e.g. body-composition metric from `BodyMeasurement`/`WeeklyCheckIn`) where the goal's metric maps to an existing source; when unlinked or no data exists, progress is 0/undefined — never fabricated.
- Owner-scoped; a goal belongs to the single MVP owner.

## Non-Functional Requirements

- **Performance**: goal list is small (personal MVP); per-request progress derivation over bounded data is acceptable.
- **Security/Privacy**: owner-scoped per ADR-002; do not bypass the owner boundary even though there is one user.
- **Explainability**: derived progress must be auditable from source data, not opaque.

## Data Model Notes

- New domain: `Goal` aggregate + `Milestone` value object, framework-free.
- `metric` is a closed enum mapping to a known source dimension (e.g. `BODY_FAT_PCT`, `WEIGHT_KG`, `LEAN_MASS_KG`); document the initial set. A metric with no source mapping is allowed but yields undefined progress.
- Reuse existing domain for derivation: `BodyMeasurement`, `WeeklyCheckIn`, `WeeklyBodySummary` — do NOT duplicate their math.
- New migration: next free version is **V11** (current head V10); one column per statement (H2/PostgreSQL convention, per V6/V7/V9 lesson). `goal` + `goal_milestone` tables (or a single table with an embedded collection — implementer's choice, documented).
- Progress is a read-model concern (derived), not a stored column.

## Edge Cases

- No goals yet → `GET` returns 200 empty list, never 404.
- Goal whose metric has no linked source/data → progress undefined/0, not an error.
- Milestone target beyond the goal target, or empty milestone list → allowed; document.
- Invalid metric or non-numeric target → 400 `VALIDATION_ERROR`, never coerced.
- Update of an unknown goal id → 404.

## Open Questions

- Exact initial `metric` enum set and each metric's source mapping — confirm against the real `BodyMeasurement`/`WeeklyCheckIn` fields before implementing derivation.
- Is milestone auto-completion (derived from progress crossing a milestone target) in scope for this slice, or is milestone state user-set only? Default to user-set unless the derivation is trivial; document the choice.
- Goal `status` (active/achieved/archived) — include a minimal status now, or defer? Keep minimal; document.
