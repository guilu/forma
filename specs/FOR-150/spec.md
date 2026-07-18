# FOR-150 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-150
Epic: FOR-148 Personalizar FORMA a Diego (MVP personal)
Origin plan: `docs/fitness_os.xlsm` — sheet **Reglas** (and Dashboard "regla práctica").
Slice 2 of 7 — **last**: depends on slice 4 (FOR-152, real catalog + cost) and slice 7 (FOR-155, check-in fields).

## Summary

Implement the 6 weekly-adjustment rules from the Excel *Reglas* sheet as explainable domain
recommendations. Today only 2 exist partially (with different thresholds and no kcal amount); 4 are
absent. Each recommendation must explain its "why" (ADR-001 — rules live in the domain, never the UI).

## The 6 rules (sheet Reglas, verified)

| # | Señal | Condición (Excel) | Acción | Por qué | Depends on |
|---|---|---|---|---|---|
| 1 | Peso baja rápido | **< −0.4 kg/semana** (absolute) | **+100/+150 kcal** | Evitar perder masa magra | — |
| 2 | Grasa sube | **2–3 semanas seguidas** | **−100 kcal** | Recorte mínimo | — (trend history) |
| 3 | Fuerza baja | **2 entrenos malos** | Semana descarga o +carbohidrato | Rendimiento manda | strength-performance signal |
| 4 | FC running alta | Mismo ritmo, **más ppm** | Dormir más / bajar intensidad | Fatiga acumulada | **FOR-155** (FC/ritmo check-in) |
| 5 | Hambre alta | **>7/10 varios días** | Más patata/verdura/proteína magra | Adherencia | **FOR-155** (hunger check-in) |
| 6 | Coste alto | **>120 €/semana** | Cambiar salmón por merluza/atún/huevos | El bolsillo también hace cardio | **FOR-152** (real catalog + cost) |

## Current repository state (verified)

- Rule 1 ≈ `BodyTrendRules.excessiveDrop` — but fires at **>1 %/week (relative)** normalized over `comparisonDays`, not the Excel **0.4 kg absolute**, and emits **no +100/+150 kcal amount**.
- Rule 2 ≈ `BodyTrendRules.worsening` — fires on **any single-week** body-fat rise (`weeklyBodyFatChange > 0`), not a **2–3 week** sustained trend, and emits **no −100 kcal amount**.
- Rules 3–6: **absent**. `RecoveryWarningRules` has a high-load/low-completion proxy and explicitly documents that "several bad/skipped sessions in a row needs per-session history the summaries do not expose — not implemented" (rule 3 gap). No FC, hunger, or cost rule exists.
- Rule engines: `domain/BodyTrendRules.java`, `domain/RecoveryWarningRules.java`, `domain/TrainingAdherenceRules.java`; assembled by `application/AdherenceService.java` into `Recommendation`s.

## Functional Requirements

- Align rules 1–2 thresholds with the Excel and add explicit kcal amounts:
  - Rule 1: trigger when weekly weight change < −0.4 kg/week (absolute, normalized over comparison days); recommendation says raise +100/+150 kcal.
  - Rule 2: trigger only on a sustained body-fat rise across 2–3 consecutive weeks (needs multi-week history, not a single delta); recommendation says −100 kcal.
- Add rules 3–6 as new explainable recommendations, each carrying its Excel "why":
  - Rule 3 (strength down, 2 bad sessions) — needs a strength-performance/"bad session" signal; document the data source (per-session strength history) and gate if absent.
  - Rule 4 (running HR high at same pace) — reads new check-in fields (FOR-155): pace (ritmo 4 km) + heart rate; gate until FOR-155 lands.
  - Rule 5 (hunger >7/10 several days) — reads a new hunger check-in field (FOR-155); gate until FOR-155.
  - Rule 6 (cost >120 €/week) — reads the weekly shopping cost vs the <120 € threshold (FOR-152); suggests swapping salmon for merluza/atún/huevos.
- Every recommendation is explainable (message + reason + source field), consistent with the existing `Recommendation` shape. No rule logic in controllers/UI (ADR-001).

## Non-Functional Requirements

- Pure, deterministic, framework-free domain rules (ADR-001), matching the existing rule engines.
- Fail-safe: missing data never fabricates a recommendation (existing convention).
- No migration required by the rules themselves (they read data other slices persist).

## Data Model Notes

- Rules 2 and 3 need **multi-week / per-session history** the current weekly summaries do not expose. Document whether this arrives via FOR-155's weekly records or an existing history source; gate the rule if the data is not yet available.
- Rules 4/5 depend on FOR-155 fields (pace, HR, hunger); rule 6 depends on FOR-152 (weekly cost + threshold). This slice sequences **after** both.

## Edge Cases

- Insufficient history (rules 2/3) → no recommendation, not a false trigger.
- Threshold boundaries: exactly −0.4 kg/week and exactly 120 € — document inclusive/exclusive per the Excel ("<−0.4", ">120").
- Multiple rules firing in one week → follow the existing precedence/aggregation model in `AdherenceService`.

## Open Questions

- Rule 3 "bad session" definition + data source (per-session strength performance vs check-in flag).
- Rule 2 sustained-trend window (2 vs 3 weeks) and where multi-week history comes from.
- Whether +100/+150 (rule 1) is one recommendation with a range or two severities.
