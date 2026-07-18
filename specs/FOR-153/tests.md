# FOR-153 Test Plan

Strict TDD: failing generator tests first (curve → intervals → deload), then implement.

## Scope

The `RunningPlanGenerator` output: volume curve, 6×400m session-2 structure, deload weeks, and days.

## Domain Tests

- 16 weeks × 3 sessions (unchanged count).
- Weekly volume follows the Excel curve 13→19 km (assert per-week or start/end + plateau at weeks 14–16 = 19 km); long run 5.0→10.0 km; session 1 = 4 km (weeks 1–4) then 5 km (weeks 5–16).
- Session 2 is a fixed 6×400m structure on non-deload weeks (chosen representation asserted).
- Deload weeks 4/8/12/16: session 2 is easy/descarga; weekly volume is non-monotone at those weeks.
- Sessions scheduled on Monday/Wednesday/Saturday (derived; consistent with FOR-151).
- Effort/pace per the Excel (RPE 6–7 range as modeled); `RunningPlanSession` invariants hold (distance > 0).
- Determinism: two calls produce identical plans.

## API Tests

- `GET /api/v1/training/running-plan` reflects the new curve, 6×400m and deload weeks.

## Edge Cases

- Week 1 starts at 13 km / 5.0 km long run (not a low ramp).
- Deload weeks dip vs the surrounding trend.
- Weeks 14–16 plateau at 19 km.

## Fixtures

- The Excel Running table (week → session distances + weekly volume + deload marker) as the expected-value fixture.
- Existing running-plan tests updated from the old 4.0→10.0 monotone curve and Tue/Thu/Sat days.
