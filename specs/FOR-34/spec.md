# FOR-34: Create late running day meal flow

Jira: https://dbhlab.atlassian.net/browse/FOR-34
Epic: FOR-4 Nutrition Planner

## Summary

Shape the RUNNING day template (FOR-33) as a late-evening-run meal flow and
surface it in the frontend: normal breakfast → solid lunch → pre-run snack →
optional post-run protein → light dinner, making clear most carbohydrates are
earlier in the day. Template behaviour + a UI that explains why.

## User/System Flow

1. User opens the Nutrition page (routed at `/nutricion`, currently a
   `PagePlaceholder`).
2. The running-day plan is shown as an ordered flow of meals, with a short
   explanation that carbs are front-loaded and dinner stays light after a late
   run.
3. Post-run protein is shown as **optional** (skip if the daily protein target
   is already met).

## Functional Requirements

- The RUNNING day template (FOR-33) includes: normal breakfast, solid lunch,
  a **pre-run snack**, an **optional post-run recovery item**, and a **light
  dinner** (dinner lighter than lunch).
- Ordering makes it obvious that carbohydrates are placed earlier
  (docs/ui-guidelines.md "Late running nutrition UX":
  Breakfast → Lunch → Pre-run snack → Run → Light recovery → Light dinner).
- The macro totals (FOR-32) for the running day still reach the target range.
- Frontend (Nutrition page) displays the running-day flow, in order, with a
  short neutral explanation of the structure.
- Post-run whey is not forced when the daily protein target is already met —
  present it as optional.
- Call the API only through the shared `apiClient` if a data source exists (see
  Open Questions); reuse existing UI primitives (`Card`, tokens); never
  duplicate calculations in the component (ADR-006).

## Non-Functional Requirements

- Practical, neutral copy — no prescription/guilt/gamified language
  (docs/ui-guidelines.md).
- Mobile-usable; loading/empty/error states where data is fetched (ADR-006).

## Data Model Notes

This is **template behaviour** over FOR-33's RUNNING template, not a strict diet
rule. **Repo gap**: no Nutrition API/read model exists yet — surfacing the flow
needs either a read endpoint or an interim data source (resolve during
implementation; see Open Questions). Do not invent an unspecified contract.

## Edge Cases

- Daily protein target already met → post-run item clearly optional (not
  implied mandatory).
- Long meal names/notes must not break the mobile layout.
- Data/API load failure → error state, not a crash (ADR-006).

## Open Questions

- **Data source**: which endpoint feeds the running-day flow? No Nutrition API
  exists yet (like the FOR-26 training gap). Options: add a read endpoint for
  the day template, or read seeded data via an interim mechanism — document the
  chosen approach; keep the frontend calling `apiClient` with a relative path.
- Whether the Nutrition page shows only the running day or all three day types
  in this slice — recommend the running-day flow first (the story's focus),
  noting the others as follow-up.
