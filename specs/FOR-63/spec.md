# FOR-63: Create notification and feedback patterns

Jira: https://dbhlab.atlassian.net/browse/FOR-63
Epic: FOR-47 UI & UX

## Summary

Define reusable notification/feedback patterns for user actions: success toast/
inline confirmation, warning, error, destructive-action confirmation, long-running
operation feedback, sync-in-progress feedback and saved/unsaved indication. Useful,
not "confetti-as-a-service". Adopted by key flows (save, complete training, sync,
recoverable errors) across modules.

## User/System Flow

No standalone screen. On a key action, the app gives immediate, consistent
feedback via the shared patterns.

## Functional Requirements

- **Success feedback**: toast or inline confirmation after key actions (save
  measurement, mark training, toggle shopping item).
- **Warning / error messages**: consistent, actionable (pairs with FOR-60 error
  states).
- **Destructive-action confirmation**: explicit confirm for delete/disconnect
  (reuse `Modal.tsx`).
- **Long-running / sync feedback**: in-progress/pending indication (integration
  sync, FOR-57).
- **Saved / unsaved indication** where relevant.
- Avoid stacking excessive notifications; accessible where practical.

## Non-Functional Requirements

- Consistent behavior across modules; calm, non-gamified copy
  (docs/ui-guidelines.md).
- Accessible: announcements via `aria-live`; confirmations keyboard-operable
  (with FOR-61).

## Data Model Notes

None — presentational notification service/component. Reuses `Modal.tsx` for
confirmations and integrates with FOR-60 error states and FOR-61 live regions.

## Edge Cases

- Rapid repeated actions → de-duplicate / limit stacked toasts.
- Destructive action cancelled → no side effect.
- Long-running action failure → transitions to an error message, not a stuck
  pending state.

## Open Questions

- Toast placement + auto-dismiss timing — recommend top/inline, dismissible, with
  a reasonable timeout for success only (persist errors); document.
- A global notification service vs per-component inline feedback — recommend a
  small provider + hook, with inline confirmation for field-level saves; document.
