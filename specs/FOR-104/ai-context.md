# FOR-104 AI Context

## Story

FOR-104 — [STUB] Progress & goals domain (streaks, achievements, adherence, photos, muscle map). Epic-sized (likely more than one epic); split into the *Proposed story slices* in `spec.md` and implement one per PR. Progress photos require a privacy review.

## Intent

Give the progress/goals experiences a real backend: goals & milestones, adherence, streaks, achievements, progress photos, and muscle-worked mapping. Success = the **Objetivos** screen (unblocks FOR-122), FOR-53 heatmap/history/streak, and FOR-56 adherence context all have data.

## Relevant Documents

- `AGENTS.md` — hexagonal boundaries, owner-scoping, never log sensitive data, no speculative abstractions.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`, `ADR-008-observability.md`.
- Mockups: `docs/3-entrenamiento.png`, `docs/6-progreso.png`, `docs/7-objetivos.png`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-104

## Domain Notes

- **No goals/achievements/adherence/streak/photo domain exists today** — this story introduces it.
- Reuse existing: `BodyMeasurement`, `WeeklyCheckIn`, `WeeklyCheckInDeltas` (FOR-110), `TrainingAdherenceRules`, `WeeklyTrainingSummary`, `Exercise`, `MovementPattern`, `StrengthWorkoutTemplate`, `SessionStatus`.
- Adherence should reuse `TrainingAdherenceRules` rather than re-deriving completion logic.
- Muscle mapping depends on whether `Exercise`/`MovementPattern` carry target-muscle data — verify; a catalog extension may be a prerequisite.
- FOR-110 already relates here (insight persistence stayed in the insights boundary); keep goals/progress in this domain boundary.

## Architectural Constraints

- Domain-first, hexagonal; thin controllers; read models separate from write aggregates.
- Achievements/adherence/streaks must be explainable/auditable from source data.
- Progress photos: private storage, access-controlled retrieval, never a public URL, content never logged, owner-scoped (ADR-002).
- New migration is **V11 or later** (head V10); one column per statement.
- No speculative abstraction beyond the current slice.

## Common Pitfalls

- Building all six areas at once — this is multiple stories; deliver one slice per PR.
- Fabricating muscle-map data an exercise catalog doesn't have.
- Division-by-zero in adherence when nothing was planned.
- Re-awarding an already-earned achievement.
- Exposing progress photos via a public/static URL or logging their content.
- Bypassing the owner boundary "because there's only one user".

## Suggested Implementation Order

1. Goals + milestones domain + persistence + API (unblocks FOR-122).
2. Adherence read model.
3. Streak + weekly-history bars.
4. Achievements.
5. Muscle-worked mapping (verify/extend `Exercise` first if needed).
6. Progress photos (privacy review).

## Validation

Run backend build + tests (`./gradlew build`). Confirm per slice: goals persist and expose derived progress; adherence returns zeros on empty windows without error; streak rule documented and tested; achievements idempotent; muscle map omits unmapped exercises; progress photos are owner-only and never publicly addressable.
