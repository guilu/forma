# FOR-63 UI Spec

## Screens

- Cross-cutting — feedback patterns used by key flows across all screens.

## Components

- Notification provider + hook (`useNotify`) rendering success/warning/error
  toasts (dedupe/limit, dismissible).
- Destructive-confirmation dialog (reuse `Modal.tsx`).
- Inline confirmation (field-level save) + saved/unsaved indicator.
- Pending/sync feedback (spinner/label), pairing with FOR-57.
- Token-driven (FOR-50); calm copy.

## States

- Loading: pending/sync in-progress feedback.
- Empty: no active notifications.
- Error: error toast/message (with FOR-60).
- Success: success toast/inline confirmation, auto-dismiss for success only.

## Interactions

- Key actions trigger feedback via the hook.
- Destructive actions open a confirm dialog (keyboard-operable, focus-trapped).
- Toasts are dismissible; success auto-dismisses, errors persist until dismissed.

## Accessibility

- Toasts/messages in an `aria-live` region; confirm dialog manages focus (FOR-61).
- Feedback conveyed by text + icon, not color alone; calm, non-gamified copy.

## Responsive Behavior

- Toasts positioned to avoid covering primary actions; readable and dismissible on
  mobile; confirm dialog full-width-friendly on small screens.
