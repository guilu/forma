# FOR-26 UI Spec

## Screens

- Training page (`frontend/src/pages/TrainingPage.tsx`) — currently a
  `PagePlaceholder`; this story replaces it with the weekly calendar.

## Components

- Weekly calendar: 7 day slots (this week), each showing its planned entries.
- Day entry: session type badge, distance (running) or workout name (strength),
  and status (planned/completed).
- Rest-day indicator for days with no session.
- Reuse `frontend/src/components/Card.tsx`; follow the header pattern used by
  the Dashboard/Progress pages (title + subtitle).

## States

- Loading: calendar area shows a loading indicator while fetching the week.
- Empty: no planned sessions this week → a clear message, not an empty grid.
- Error: plan/API load failure → a clear error state (ADR-006).
- Success: the week populated with running/strength/rest entries.

## Interactions

- Read-only in this story: no marking complete (that is FOR-27), no plan
  editing.
- Recommended MVP: current week only, no week navigation (see spec.md Open
  Questions).

## Accessibility

- Each day and entry has a text label (type, distance/name, status) — not
  conveyed by color/icon alone.
- Status is announced as text; empty/error states are announced to screen
  readers.
- Keyboard-reachable; visible focus states via existing tokens.

## Responsive Behavior

- Mobile: days stack vertically, readable, no horizontal scroll (ui-guidelines:
  keep it simple).
- Desktop: may lay the 7 days as a row/grid, using the same data/entry model as
  mobile.
