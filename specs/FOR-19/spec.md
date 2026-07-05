# FOR-19: Create body dashboard metric cards

Jira: https://dbhlab.atlassian.net/browse/FOR-19
Epic: FOR-2 Body Composition

## Summary

Replace the current placeholder `DashboardPage`
(`frontend/src/pages/DashboardPage.tsx`) with metric cards for weight, body
fat %, fat mass, lean mass and BMI, using the latest `BodyMeasurement` from
the FOR-17 API. Clear empty state when there are no measurements yet; round
values sensibly.

## User/System Flow

1. User opens the Dashboard (already routed, currently a `PagePlaceholder`
   that explicitly says it "will be built when its data sources exist" — the
   FOR-17 API is that data source).
2. Frontend calls `GET /api/v1/body/measurements` (ordered newest-first per
   FOR-16/FOR-17) and takes the first item as the latest measurement.
3. Dashboard renders one card per metric: weight, body fat %, fat mass, lean
   mass, BMI.
4. If the list is empty, the dashboard shows an empty state instead of cards
   with placeholder/zero values.

## Functional Requirements

- Cards for: weight (`weightKg`), body fat % (`bodyFatPercentage`), fat mass
  (`fatMassKg`), lean mass (`leanMassKg`), BMI (`bmi`) — all from the latest
  measurement returned by `GET /api/v1/body/measurements`
  (`specs/FOR-17/api.md`).
- Reuse `frontend/src/components/Card.tsx` per existing dashboard widget
  pattern (docs/ui-guidelines.md "Dashboard widgets" already lists these
  exact metrics).
- Follow docs/ui-guidelines.md layout principles: cards for metrics, answer
  "weekly status" quickly, no noisy visuals.
- Round displayed values sensibly — e.g. one decimal for weight/body fat %,
  one decimal or whole units for masses/BMI — and never show more precision
  than the input data supports (docs/ui-guidelines.md: "No fake precision").
- Empty state (no measurements yet) replaces all cards with a single clear
  message, not five empty/zero cards.

## Non-Functional Requirements

- No domain calculation duplicated in the frontend — fat mass/lean mass
  values are read from the API response only (ADR-006).
- Mobile-usable, though desktop is explicitly allowed to prioritize the full
  dashboard (docs/ui-guidelines.md "Mobile considerations": mobile
  prioritizes today's plan / add measurement / mark training / shopping
  checklist over the full dashboard).

## Data Model Notes

Card data comes directly from one `GET /api/v1/body/measurements` response
item (`specs/FOR-17/api.md`); no separate backend endpoint is introduced by
this story unless FOR-17's existing list endpoint proves insufficient during
implementation (see Open Questions).

## Edge Cases

- Zero measurements — show the empty state.
- Exactly one measurement — dashboard still renders normally (latest = only
  item).
- A measurement missing an optional-ish value (if any field is later made
  optional) — card shows a clear "no data" marker for that metric rather
  than `0` or `NaN`.

## Open Questions

- The Jira DoD says "backend endpoint supports dashboard data if needed" —
  FOR-17's existing `GET /api/v1/body/measurements` already returns
  everything this story needs (weight, body fat %, fat mass, lean mass,
  BMI) from the latest item, so no new backend endpoint is expected. Revisit
  only if implementation finds the existing endpoint insufficient (e.g.
  performance at scale), and document that decision if it happens.
