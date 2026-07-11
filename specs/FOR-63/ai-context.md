# FOR-63 AI Context

## Story

FOR-63 — Create notification and feedback patterns
(https://dbhlab.atlassian.net/browse/FOR-63)

## Intent

Give users immediate, consistent feedback when they save data, complete training,
sync or hit recoverable problems. Success is a reusable feedback set (success/
warning/error/confirm/pending/saved) adopted by key flows — useful, not noisy.

## Relevant Documents

- `AGENTS.md`
- `docs/ui-guidelines.md` (calm, no confetti/guilt), `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-60/` (error states), `specs/FOR-61/` (a11y), `specs/FOR-57/` (sync)
- Jira: https://dbhlab.atlassian.net/browse/FOR-63

## Domain Notes

- Reuse `components/Modal.tsx` for destructive confirmations.
- Pairs with FOR-60 (error surfaces) and FOR-61 (`aria-live` announcements).
- Key flows needing feedback: measurement save (FOR-52), training completion
  (FOR-53), shopping toggle (FOR-55), integration sync (FOR-57).

## Architectural Constraints

- Small notification provider + hook (or inline confirmation) under
  `frontend/src/components/`; presentational. No feature logic. Token-driven
  (FOR-50).

## Common Pitfalls

- Notification spam / stacking; gamified copy.
- Destructive actions without explicit confirmation.
- Pending state that never resolves on failure.

## Suggested Implementation Order

1. Notification provider + hook (success/warning/error toasts) with dedupe/limit.
2. Destructive-confirmation pattern (reuse `Modal`).
3. Pending/sync + saved/unsaved indicators.
4. Adopt in key flows; a11y announcements; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise key flows for consistent feedback.
