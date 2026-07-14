# FOR-122: Objetivos screen

Jira: https://dbhlab.atlassian.net/browse/FOR-122
Epic: FOR-47 UI & UX

## Summary

`/objetivos` currently routes to `GoalsPage.tsx`, which renders nothing but
`<PagePlaceholder title="Objetivos" />` — verified directly against the
source. The only goals-related content anywhere today is a read-only
"Objetivo principal" row inside `ProfileSection` (Ajustes) and a set of
inert `ObjectivesSection` rows (déficit calórico, proteínas, agua diaria) —
both explicitly documented as standing in for a screen that "has no
dedicated FOR-47 child story" (FOR-58's spec Data Model Notes). This story
is that dedicated screen: a real Objetivos page consuming the progress &
goals domain, with goal listing, detail and edit.

## User/System Flow

1. User navigates to Objetivos (`/objetivos`, already in `NAV_ITEMS`).
2. The page lists the user's goals (sourced from the FOR-104 progress &
   goals backend).
3. User opens a goal's detail view; can edit it (target value, deadline,
   etc., per whatever FOR-104 exposes).
4. Changes persist through FOR-104's commands; feedback via FOR-63.

## Functional Requirements

- Replace `GoalsPage`'s `PagePlaceholder` with a real screen: goal listing
  (cards or list rows, consistent with the rest of the app's `Card`
  patterns), a detail view per goal, and an edit flow.
- Consume the FOR-104 progress & goals domain's read model for listing and
  detail; consume its commands for edit. **FOR-104 has no spec folder in
  this repository as of this story's authoring** — verified
  (`specs/FOR-104/` does not exist). Per AGENTS.md ("do not invent
  requirements when the backlog or spec is silent"), this spec does not
  fabricate FOR-104's exact API shape; implementation must read FOR-104's
  actual spec/API (once it exists) before building against it, and should
  flag here if FOR-104 is still missing when this story starts.
- Retire the redundant read-only "Objetivo principal" summary in
  `ProfileSection` and the inert `ObjectivesSection` rows in favor of
  linking to this new screen, or keep them as a compact summary that links
  through — decide during implementation and document (avoid presenting
  the same data as both a dead-end summary and a real screen with no
  connection between them).
- Loading/empty/error states (FOR-60): no goals yet → `EmptyState` with a
  clear call to action to create one (if FOR-104 supports creation) or a
  calm "no goals set" message if creation isn't in scope.

## Non-Functional Requirements

- Section-based layout consistent with the rest of the app (ADR-006).
- No goal-progress math client-side — the FOR-104 read model is the source
  of truth for progress percentages/status (architecture-overview.md).

## Data Model Notes

Depends entirely on FOR-104's domain shape, which does not exist in this
repository yet. This is the single largest open dependency in this batch —
document any assumption made during implementation clearly, and prefer
scoping down (e.g. list + detail only, defer edit) over inventing backend
behavior if FOR-104 lands with a narrower scope than expected.

## Edge Cases

- No goals exist yet → empty state, not a blank page (distinguishing "no
  data" from "still loading" per FOR-60).
- FOR-104 not yet available when this story is picked up → do not fake a
  backend; keep `GoalsPage` as a documented placeholder and flag the
  blocker explicitly rather than building against invented endpoints
  (AGENTS.md: "Repository state has priority over roadmap/spec intent").

## Open Questions

- Exact goal fields/edit capabilities — entirely dependent on FOR-104's
  eventual spec; this story's scope may need re-cutting once that spec
  exists.
- Whether ProfileSection's "Objetivo principal" and ObjectivesSection's
  rows are removed, kept as summaries linking here, or left untouched
  until a separate cleanup story — recommend linking, but confirm during
  implementation.
