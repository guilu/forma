# FOR-153 AI Context

## Story

FOR-153 — Plan de running real (13→19 km, 6×400m, deload). Slice 5 of epic FOR-148. In-code
generator change; runs after FOR-151 (days).

## Intent

Make `RunningPlanGenerator` produce Diego's real 16-week plan: 13→19 km/week, session 2 = 6×400m,
deload every 4 weeks. Success = the generated plan matches the Running sheet's volume curve,
interval structure and deload weeks, with days still derived (Mon/Wed/Sat).

## Relevant Documents

- `AGENTS.md` — hexagonal, deterministic domain, single source of truth.
- `docs/fitness_os.xlsm` — sheet **Running** (16-week table). Source of truth for volume/structure/deload.
- `docs/adr/ADR-001-architecture.md` (deterministic domain, no logic in UI), `ADR-002-authentication.md` (owner-scoping), `ADR-003-persistence.md` (no migration).
- Upstream: `specs/FOR-151/` (running days Mon/Wed/Sat derive from this generator).
- Jira: https://dbhlab.atlassian.net/browse/FOR-153

## Domain / Repo Notes (verified)

- `RunningPlanGenerator.sixteenWeekPlan()` — long run linear 4.0→10.0, easy=0.55×long, intervals=0.6×long; RPE 4/7/5; **monotone increasing, no deload, no fixed 6×400m**.
- `RunningPlanSession` record: `weekNumber`, `dayOfWeek`, `SessionType{EASY,INTERVALS,LONG_RUN}`, `targetDistanceKm` (>0), `targetEffort` [1,10], `notes`. Validates positivity/effort range.
- `WeeklyTrainingDayPolicy.RUNNING_DAYS` derives from this generator's session days — FOR-151 changes those to Mon/Wed/Sat.
- In-code, no persistence/migration (head V19).

## Architectural Constraints

- Pure, deterministic (ADR-001) — same call, same plan; no framework types.
- Keep the days coming from the generator (single source with FOR-151); do not add a separate day literal.
- No migration (ADR-003; head stays V19).
- Prefer the minimal model change for 6×400m / deload (notes or a small optional field), not a redesign.

## Common Pitfalls

- Keeping the monotone-increase invariant — deload weeks (4/8/12/16) must dip.
- Modeling intervals as a % of the long run — session 2 is a fixed 6×400m structure.
- Starting week 1 below 13 km — the Excel week 1 already sits at 13 km / 5.0 km long run.
- Hardcoding running days here or elsewhere instead of relying on the derived days.
- `targetDistanceKm` must stay strictly positive (record invariant) — a deload/interval must still carry a positive distance.

## Suggested Implementation Order

1. Recompute the long-run curve (5.0→10.0) + session-1 (4→5 km) to hit the 13→19 km weekly target.
2. Make session 2 a fixed 6×400m structure (chosen representation) with the Excel pace/RPE.
3. Add deload weeks 4/8/12/16 (session 2 easy/descarga; non-monotone curve).
4. Confirm days still derive to Mon/Wed/Sat (FOR-151) and update generator tests.

## Validation

Backend build + tests (`./gradlew build`). Confirm: weekly volume 13→19 km per the Running sheet; session 2 is 6×400m except deload weeks; deload at weeks 4/8/12/16; long run 5.0→10.0; days derive Mon/Wed/Sat; deterministic; no migration (head V19); `RunningPlanSession` invariants respected.
