# FOR-153 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-153
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheet **Running** (16-week plan).
Slice 5 of 7. After slice 3 (FOR-151) which fixes the running days.

## Summary

Adjust the running-plan generator to Diego's real plan: weekly volume **13 → 19 km**, session 2 as a
fixed **6×400m** interval structure, and a **deload every 4 weeks**. Today the volume curve, the
interval structure and the (missing) deload all differ.

## Excel plan (sheet Running, verified — 16 weeks, 3 sessions/week)

- **Session 1 (suave)**: 4 km (weeks 1–4), 5 km (weeks 5–16). Ritmo 5:45–6:15/km, RPE 6–7.
- **Session 2 (series cortas)**: **6×400m** — except deload weeks, where it is "Rodaje muy suave/descarga".
- **Session 3 (largo)**: 5.0 → 10.0 km, conversacional.
- **Weekly volume (Km semana)**: 13.0, 13.4, 13.8, 14.2, 15.6, 16.0, 16.4, 16.8, 17.2, 17.6, 18.0, 18.4, 18.8, 19.0, 19.0, 19.0 → **13 → 19 km**.
- **Deload every 4 weeks**: weeks **4, 8, 12, 16** ("Rodaje muy suave/descarga"; not a monotone increase).
- Notes column: "Semana descarga cada 4 semanas"; objetivo "Acumular sin lesionarse" → "Consolidar 8–10 km".

## Current repository state (verified)

- `domain/RunningPlanGenerator.java` — 16 weeks × 3 sessions ✓, but: long run **4.0 → 10.0 km linear/monotone**; easy = 0.55 × long; intervals = 0.6 × long; RPE easy 4 / intervals 7 / long 5. Weekly volume ≈ 8.6 → 21.5 km. **No 6×400m fixed structure, no deload weeks** (distance never decreases — documented explicitly in the class).
- `domain/RunningPlanSession.java` — record `(weekNumber, dayOfWeek, SessionType{EASY,INTERVALS,LONG_RUN}, targetDistanceKm>0, targetEffort∈[1,10], notes)`. No interval-structure field; no deload flag.
- Running days come from this generator's session days and are corrected to **Mon/Wed/Sat** by FOR-151 (single source).
- In-code generator, no persistence/migration (head stays V19).

## Functional Requirements

- Volume progression targets **13 → 19 km/week** following the Excel curve (long run 5.0 → 10.0 km; session 1 4 km then 5 km).
- Session 2 = fixed **6×400m** interval structure (not a % of the long run). Represent the 6×400m (e.g. `targetDistanceKm ≈ 2.4` + a structured `notes`/interval descriptor); document the chosen representation.
- Introduce **deload weeks 4/8/12/16**: session 2 becomes an easy/descarga run and the curve is non-monotone at those weeks (remove the "monotonically increasing" invariant).
- Running days remain **derived from this generator** (Mon/Wed/Sat per FOR-151) — do not hardcode days elsewhere.
- Paces/effort per the Excel: suave 5:45–6:15/km, RPE 6–7, largo conversacional (keep effort on the RPE scale the record already uses).

## Non-Functional Requirements

- Pure, deterministic, framework-free generator (ADR-001) — same call, same plan.
- In-code plan (no migration, ADR-003; head stays V19) consistent with the existing generator.
- Single source of truth for running days preserved (FOR-151).

## Data Model Notes

- Representing **6×400m** may need a small extension: either encode it in `notes` (MVP) or add an
  optional interval descriptor to `RunningPlanSession`. Document the choice; prefer the minimal one.
- **Deload** may be modeled via reduced effort/distance on the session-2 slot + a note, or a new
  flag/session-type. Prefer the minimal representation that removes the monotone-increase assumption.
- The Excel's "Km semana" (13→19) is a stated weekly target that may not equal the literal sum of the
  three session distances; reconcile in design (target vs derived) and document which the generator emits.

## Edge Cases

- Deload weeks (4/8/12/16): session 2 is easy/descarga, not 6×400m; weekly volume dips vs the prior week's trend.
- Weeks 14–16 plateau at 19 km / 10 km long run (no further increase).
- Week 1 already starts at 13 km (not a low ramp) — the generator must not start below the Excel's week-1 volume.

## Open Questions

- Interval-structure representation for 6×400m (notes vs a structured field).
- Deload representation (flag vs session-type vs reduced effort).
- Whether "Km semana" is emitted as a target field or derived from session distances.
