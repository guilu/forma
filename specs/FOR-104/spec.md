# FOR-104 Spec

> ⚠️ **Epic-sized stub (likely more than one epic).** Progress photos are
> privacy-sensitive. This folder captures full scope and proposes slicing; it does
> NOT create Jira issues. Implement one slice per PR with `jira-sdd-ai`.

Jira: https://dbhlab.atlassian.net/browse/FOR-104
Epic: FOR-96 UI Backend Enablers — Foundations

## Summary

Domain + persistence + API for the progress/goals experiences the mockups imply but
that have no data today: **goals & milestones**, **adherence/consistency** (planned vs
completed over N days), **streaks**, **achievements/logros**, **progress photos**
(privacy-sensitive), and **muscle-worked mapping** for strength sessions. This
unblocks the **Objetivos** screen (docs/7-objetivos.png, currently unowned in FOR-47),
the FOR-53 muscle heatmap / weekly-history / streak, and FOR-56 progress/adherence
context.

## User/System Flow

1. User opens Objetivos (FOR-47) → GET goals with progress and milestones.
2. User creates/edits a goal with milestones → command persists it.
3. Progreso (mockups 3/6) → GET adherence (30-day planned vs completed for entrenamientos/nutrición/mediciones), current streak, achievements, weekly-history bars.
4. User uploads a progress photo → stored privately; GET returns a private, access-controlled reference (not a public URL).
5. Strength session detail → GET muscle-worked map derived from the session's exercises.

## Functional Requirements

- **Goals + milestones**: create/read/update goals, each with milestones and progress derived from real data (measurements/training/nutrition) where possible.
- **Adherence read model**: planned vs completed counts over a rolling window (default 30 days) per category, reusing `TrainingAdherenceRules` where applicable.
- **Streak calculation**: consecutive-period consistency from existing activity/measurement history.
- **Achievements/logros**: rule-driven awards from existing data; persisted once earned.
- **Progress photos**: private storage with owner-scoped, access-controlled retrieval; never a public URL; content never logged.
- **Muscle-worked mapping**: derive worked-muscle data for strength sessions from `Exercise`/`MovementPattern`/`StrengthWorkoutTemplate`.

## Non-Functional Requirements

- **Privacy** (primary for photos): progress photos are sensitive personal data — private storage, access-controlled references, no content in logs, owner-scoped (ADR-002).
- **Performance**: adherence/streak read models query bounded windows; acceptable per-request computation at MVP volume.
- **Explainability**: achievements and adherence must be derivable/auditable from source data, not opaque.

## Data Model Notes

- New domain: `Goal` (+ `Milestone`), `Achievement`, and read models for `Adherence`, `Streak`, `WeeklyHistory`, `MuscleWorkedMap`.
- Reuse existing: `BodyMeasurement`, `WeeklyCheckIn`, `TrainingAdherenceRules`, `WeeklyTrainingSummary`, `Exercise`, `MovementPattern`, `StrengthWorkoutTemplate`, `SessionStatus`.
- Muscle mapping likely derives from `Exercise` + `MovementPattern` — verify these carry (or can carry) target-muscle data; if not, a catalog-extension slice is a prerequisite.
- Progress photos: metadata in the DB; binary in a private store (adapter). No public/static hosting.
- New migration(s): **V11+** (head V10); one column per statement.
- Achievements: derived-then-persisted; streaks/adherence are read models computed on demand.

## Edge Cases

- Goal with no linked data yet → progress 0/undefined, not an error.
- Adherence window with no planned items → return zeros, not division-by-zero.
- Streak across a gap day → resets per the defined rule; document the rule.
- Achievement already earned → not re-awarded/duplicated (idempotent).
- Progress photo access by anything other than the owner → denied (even in single-user MVP, do not bypass the boundary).
- Exercise with no muscle mapping data → omit from the map, do not fabricate.

## Proposed story slices

> **Status (updated 2026-07-18):** slices 1, 2, 4, 5 are DONE. Only **3** and **6**
> remain. This stub was written before slices 1/2 shipped — verify repo state
> before creating new slice issues (goals/adherence already exist).

1. ✅ **DONE (FOR-125)** — Goals + milestones domain + persistence + API (`Goal`/`Milestone`, `V11__goal.sql`, `GoalController`). Unblocks the Objetivos screen (FOR-122).
2. ✅ **DONE (FOR-129)** — Adherence read model API (`AdherenceService`, `GET /api/v1/progress/adherence?days=`). Unblocks FOR-56 context.
3. ⬜ **FOR-139** — Streak calculation + weekly-history bars API — unblocks FOR-53 streak/history. NOTE: `training_session_status` stores only current status per weekday (no per-date training history) — streak derives from nutrition/measurement dates.
4. ✅ **DONE (FOR-135)** — Achievements/logros (rule-driven, persisted).
5. ✅ **DONE (FOR-136)** — Muscle-worked mapping API. `Exercise.primaryMuscles` already existed; no catalog extension needed. Unblocks FOR-53 heatmap.
6. ⬜ **FOR-140** — Progress photos storage + private retrieval (privacy review required).

## Open Questions

- Do `Exercise`/`MovementPattern` carry target-muscle data today, or is a catalog extension a prerequisite for the muscle map?
- Exact streak rule (what counts as a "consistent" period; how gaps reset).
- Where do progress-photo binaries live (private object store vs DB blob) — decide in slice 6's design, consistent with ADR-003.
- Which achievements exist for MVP and their exact rules.
- Adherence window length (default 30 days) and category set — confirm against mockups 3/6.
