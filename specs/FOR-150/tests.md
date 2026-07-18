# FOR-150 Test Plan

Strict TDD: failing tests first (per-rule domain thresholds → assembly → API content), then implement.

## Scope

The 6 *Reglas* rules as explainable domain recommendations: rules 1–2 re-thresholded with amounts,
rules 3–6 added and gated on their data sources.

## Domain Tests

- Rule 1: weekly weight change < −0.4 kg/week fires; exactly −0.4 does not (document inclusive/exclusive per Excel "<−0.4"); message includes +100/+150 kcal.
- Rule 2: 2–3 consecutive weeks of body-fat rise fires with −100 kcal; a single week does not.
- Rule 3: 2 "bad" strength sessions fire; fewer do not; no data → no recommendation.
- Rule 4: same pace + higher HR fires; missing FC/pace (pre-FOR-155) → no recommendation.
- Rule 5: hunger >7/10 across several days fires; missing hunger field → no recommendation.
- Rule 6: weekly cost > 120 € fires with the salmon→merluza/atún/huevos message; exactly 120 € does not (Excel ">120"); missing cost (pre-FOR-152) → no recommendation.
- Each recommendation carries the Excel "why" as its reason.

## Application Tests

- `AdherenceService` assembles the new rules with a documented, deterministic precedence.
- Multiple rules in one week produce the expected combined/prioritized output.

## API Tests

- `GET /api/v1/insights` reflects the new thresholds/amounts and includes the new rules when their data is present.
- Gated rules are absent (not errored) when their source data is missing.

## Edge Cases

- Boundary values −0.4 kg/week and 120 €/week.
- Insufficient multi-week/per-session history → no false trigger.
- Data present for rule 6 (post-FOR-152) and rules 4/5 (post-FOR-155).

## Fixtures

- Weekly summaries / multi-week history hitting each boundary.
- A weekly cost above and below 120 € (aligned with FOR-152).
- Check-in fixtures with/without FC, pace and hunger (aligned with FOR-155).
