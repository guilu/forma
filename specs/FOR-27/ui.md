# FOR-27 UI Spec

## Screens

- Weekly training calendar (FOR-26, `frontend/src/pages/TrainingPage.tsx`) —
  this story adds the "mark status" action to each session entry.

## Components

- Per-session status control: mark **completed** / **skipped** (and, if allowed,
  revert to planned) on a calendar entry.
- Optional notes input on completion (if surfaced in this story).
- Reuse existing UI primitives (`Card`, buttons, design tokens); follow the
  FOR-18 form/interaction patterns.

## States

- Loading: the session entry shows a pending state while the update is in
  flight; the control is disabled to prevent double submit.
- Empty: N/A (acts on existing sessions).
- Error: update failure shows a clear message near the entry, using
  `ApiError.message` (not raw detail), consistent with FOR-18.
- Success: the entry reflects the new status immediately.

## Interactions

- Marking a session updates its status via `apiClient` (relative `/api/...`).
- After a successful update, the calendar entry’s status is refreshed.
- Keep it simple: no bulk edit, no detailed workout logging.

## Accessibility

- Status controls are real buttons with text labels (not icon-only).
- Status is announced as text; the updated state is perceivable without color
  alone.
- Keyboard-operable with visible focus states.

## Responsive Behavior

- Mobile: status controls are touch-friendly within the stacked calendar; no
  horizontal scroll.
- Desktop: same controls within the day/grid layout.
