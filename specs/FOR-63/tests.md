# FOR-63 Test Plan

## Scope

Verify the feedback patterns: success/warning/error, destructive confirmation,
pending/sync feedback and saved state, adopted consistently and accessibly.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- A key action triggers a success confirmation (toast/inline).
- A failed action shows a clear error message.
- A destructive action requires explicit confirmation; cancel has no effect.
- A long-running/sync action shows pending feedback and resolves to success or
  error.
- Notifications do not stack excessively (dedupe/limit).
- Feedback is announced via `aria-live` (with FOR-61).

## Edge Cases

- Rapid repeated actions → limited/deduped toasts.
- Cancelled destructive action → no side effect.
- Long-running failure → error, not a stuck pending state.

## Fixtures

- A component wired to the notification hook; success/error/confirm/pending
  scenarios.
