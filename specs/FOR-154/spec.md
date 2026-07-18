# FOR-154 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-154
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheet **Fuerza**.
Slice 6 of 7. After slice 3 (FOR-151) which fixes the strength days (Mar/Jue/Dom).

## Summary

Adjust the strength templates to Diego's real plan: **5 exercises per block** with **per-exercise**
series/reps/RIR/rest, replacing today's 3-exercises-per-block uniform scheme. Add the missing
catalog exercises and model the rep schemes the Excel uses (fixed range, AMRAP, time-based holds).

## Excel plan (sheet Fuerza, verified — 3 blocks × 5 exercises)

**Empuje (Martes)**
| Ejercicio | Series | Reps | RIR | Descanso |
|---|---|---|---|---|
| Press mancuernas en banco | 4 | 8–12 | 2 | 90s |
| Press militar mancuernas | 3 | 8–10 | 2 | 90s |
| Flexiones | 3 | AMRAP | 1 | 60s |
| Elevaciones laterales | 3 | 12–20 | 2 | 45s |
| Plancha | 3 | 45–75s | 2 | 45s |

**Tirón (Jueves)**
| Ejercicio | Series | Reps | RIR | Descanso |
|---|---|---|---|---|
| Dominadas agarre puerta | 4 | AMRAP | 1 | 120s |
| Remo mancuerna a banco | 4 | 8–12 | 2 | 90s |
| Face pull con banda | 3 | 15–25 | 2 | 45s |
| Curl bíceps | 3 | 10–15 | 2 | 60s |
| Pájaros posteriores | 3 | 12–20 | 2 | 45s |

**Pierna/Core (Domingo)**
| Ejercicio | Series | Reps | RIR | Descanso |
|---|---|---|---|---|
| Sentadilla goblet | 4 | 10–15 | 2 | 90s |
| Peso muerto rumano mancuernas | 4 | 8–12 | 2 | 90s |
| Zancadas | 3 | 10–12/pierna | 2 | 90s |
| Gemelos | 4 | 15–25 | 1 | 45s |
| Dead bug / hollow hold | 3 | 10–15 | 2 | 45s |

Each row also has a progresión + notas column (e.g. "cuando llegues a 12 sube peso").

## Current repository state (verified)

- `domain/WorkoutTemplateCatalog.java` — 3 blocks with only **3 exercises each** (Legs has 4), **uniform** `SETS=3, RIR=2`, reps 8–12 (compound) / 10–15 (core), rest 90/45s. No per-exercise programming.
- `domain/ExerciseCatalog.java` — **11 exercises**. Present and reusable: push-up (Flexiones), dumbbell-shoulder-press (Press militar), pull-up (Dominadas), dumbbell-row (Remo), band-face-pull (Face pull), goblet-squat, dumbbell-rdl (PM rumano), reverse-lunge (Zancadas), plank (Plancha), dead-bug. **Missing: Press mancuernas en banco (dumbbell bench press), Elevaciones laterales, Curl bíceps, Pájaros posteriores (rear-delt), Gemelos (calf raise).**
- `domain/StrengthWorkoutItem.java` — record `(exerciseId, order, sets, repsMin, repsMax, restSeconds, rir)`. **RIR and restSeconds ALREADY exist per item** (contradicts the Jira "posible extensión para RIR/descanso si no existen" — they exist). The real gaps: `repsMin/repsMax` are ints ≥1 with `repsMax≥repsMin`, so **AMRAP** (Flexiones/Dominadas) and **time-based holds** (Plancha 45–75s) cannot be represented; per-side reps (Zancadas /pierna) is a note.
- In-code catalog, no persistence/migration (head stays V19).

## Functional Requirements

- Add the ~5 missing exercises to `ExerciseCatalog` (dumbbell bench press, lateral raise, biceps curl, rear-delt/reverse fly, calf raise), each with `primaryMuscles` and home `Equipment` (mancuernas/banco/bandas/barra).
- Rebuild the 3 templates with **5 exercises each** and **per-exercise** sets/reps/RIR/rest from the Fuerza table (RIR/rest already supported per item — just populate them non-uniformly).
- Model the rep schemes the Excel needs: **fixed range** (existing), **AMRAP**, and **time-based hold (seconds)**. Extend `StrengthWorkoutItem` minimally (e.g. an optional rep-scheme/kind + optional time field) rather than forcing everything into `repsMin/repsMax`. Preserve existing invariants for the range case.
- Strength days stay Mar/Jue/Dom (FOR-151); keep `WorkoutType` PUSH/PULL/LEGS (= Empuje/Tirón/Pierna-Core).

## Non-Functional Requirements

- Pure, framework-free domain (ADR-001); in-code catalog (no migration, ADR-003; head stays V19).
- `WorkoutTemplateCatalog` keeps its fail-fast check that every `exerciseId` exists in `ExerciseCatalog`.
- Explainable: every exercise/programming value traceable to the Fuerza sheet; never fabricated.

## Data Model Notes

- `StrengthWorkoutItem` extension is the crux: today it cannot express AMRAP or a 45–75s hold. Options: a `RepScheme`/kind enum (RANGE / AMRAP / TIME) with optional bounds and an optional `durationSecondsMin/Max`, keeping the current range fields for RANGE. Document the chosen shape; keep it minimal and backward-compatible with existing items.
- Per-side reps ("10–12/pierna") can live in a note/flag; do not overload `repsMax`.
- Progresión/notas text can map to the existing `Exercise.instructions` or an item note; decide and document.

## Edge Cases

- AMRAP items (Flexiones, Dominadas): no fixed rep upper bound — must not break the `repsMax≥repsMin` invariant path.
- Time-based hold (Plancha 45–75s): seconds, not reps.
- Fail-fast: a template referencing a not-yet-added exercise id must fail at load (existing behavior).

## Open Questions

- Exact `StrengthWorkoutItem` extension for AMRAP/time schemes (enum + optional fields vs separate types).
- Whether progresión/notas is stored per item or per catalog exercise.
- Stable ids/`primaryMuscles` wording for the new exercises (consistent with the FOR-136 muscle-map derivation).
