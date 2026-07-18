# FOR-150 AI Context

## Story

FOR-150 — Reglas de ajuste semanal (the 6 rules of the *Reglas* sheet). Slice 2 of epic FOR-148,
sequenced **last** (depends on FOR-152 for cost and FOR-155 for check-in fields).

## Intent

Make the app's weekly recommendations match Diego's real adjustment rules. Success = the 6 *Reglas*
rules exist as explainable domain recommendations with the Excel thresholds and kcal amounts; rules
gated on data that other slices provide until those slices land.

## Relevant Documents

- `AGENTS.md` — hexagonal; business rules live in the domain, never in controllers/UI.
- `docs/fitness_os.xlsm` — sheet **Reglas** (6 rules) + **Dashboard** "regla práctica" summary. Source of truth for thresholds and "why".
- `docs/adr/ADR-001-architecture.md` (rules in domain, not UI), `ADR-002-authentication.md` (owner-scoping).
- Sibling slices: `specs/FOR-152/` (cost + threshold, rule 6), `specs/FOR-155/` (FC/pace/hunger, rules 4–5).
- Jira: https://dbhlab.atlassian.net/browse/FOR-150

## Domain / Repo Notes (verified)

- `BodyTrendRules` — `WEEKLY_DROP_LIMIT_PCT = 1.0` (relative %/week, exclusive); `excessiveDrop` (rule 1 proxy) and `worsening` (rule 2 proxy) emit no kcal amount. Reads `WeeklyBodySummary` deltas + `comparisonDays`.
- `RecoveryWarningRules` — `HIGH_LOAD_SESSIONS = 5`, `LOW_COMPLETION = 0.4`; documents the per-session-history gap (rule 3 needs data not yet exposed).
- `TrainingAdherenceRules` — adherence bands; unrelated to the 6 *Reglas* directly but same `Recommendation` pattern.
- `application/AdherenceService.java` — assembles rule outputs into recommendations (precedence/aggregation lives here).
- `Recommendation(createdAt, category, severity, message, reason, sourceField)` — the explainable output shape.

## Architectural Constraints

- Pure, deterministic, framework-free (ADR-001). Same `Recommendation` shape; keep messages Spanish, explainable (message + reason + source).
- Fail-safe: no data → no recommendation (never fabricate).
- No new migration for the rules; they read data persisted by FOR-152 (cost) and FOR-155 (check-in).
- Gate rules 4/5/6 (and possibly 3) behind their data source; document the dependency rather than inventing fields here.

## Common Pitfalls

- Re-using the current relative %/week threshold for rule 1 — the Excel is **0.4 kg absolute/week**.
- Firing rule 2 on a single week — it requires a **2–3 week sustained** body-fat trend.
- Inventing FC/hunger/cost fields in this slice — they belong to FOR-155/FOR-152; depend on them.
- Putting rule logic or thresholds in the frontend/controllers (ADR-001).
- Forgetting the explicit kcal amounts (+100/+150, −100) the Excel mandates.

## Suggested Implementation Order

1. Re-thresholds rules 1–2 to the Excel values + add kcal amounts (domain, tested at boundaries).
2. Add rule 6 (cost >120 €) once FOR-152's weekly cost + threshold exist.
3. Add rules 4–5 (FC/pace, hunger) once FOR-155's check-in fields exist.
4. Add rule 3 (strength down) against the chosen strength-performance signal; gate if absent.
5. Wire all into `AdherenceService` with a documented precedence.

## Validation

Backend build + tests (`./gradlew build`). Confirm: thresholds/amounts match the *Reglas* sheet; boundary tests for −0.4 kg and 120 €; multi-week/per-session gaps handled without false triggers; rules 4/5/6 read the FOR-155/FOR-152 data; every recommendation is explainable; no rule logic outside the domain.
