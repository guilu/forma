# FOR-115 Test Plan

## Scope

Verify the new "Soporte y ayuda" section renders its entries correctly,
inert entries are non-interactive, and it's mounted in `SettingsPage`.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — no backend for this story.

## UI Tests

- `SupportSection` renders a `Card` titled "Soporte y ayuda" with the
  mockup-derived entries.
- Any entry without a real destination renders as `inert` (matching
  `SettingsRow`'s `inert` prop contract, mirrored on `AboutSection`'s
  existing tests).
- `SettingsPage` renders `SupportSection` alongside the existing sections,
  in the mockup's relative order.

## Edge Cases

- No entries have a real backend/page behind them yet → every row renders
  inert, and the section still renders without error (not omitted
  entirely).

## Fixtures

None required — static content, same as `AboutSection.test.tsx`.
