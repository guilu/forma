# FOR-116 Test Plan

## Scope

Verify each provider renders its brand logo (not the old generic icon), the
fallback path works for a provider without an asset, and both render sites
(connected + available lists) are updated consistently in both themes.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — no API change.

## UI Tests

- Each of `WITHINGS`, `GOOGLE_FIT`, `APPLE_HEALTH` renders its brand logo
  asset instead of the previous generic `Icon` name (`heart`/`activity`/
  `cross`).
- Both render sites in `IntegrationsSection` (connected providers list,
  available providers list) render the same updated logo per provider.
- A provider with no available brand asset falls back to the documented
  generic icon, not a broken image/empty slot.
- Each provider row retains an accessible name (visible text or
  `aria-label`) independent of the logo image.

## Edge Cases

- Dark theme rendering: each logo remains legible (matches `themedRendering.
  test.tsx`'s existing dual-theme testing pattern for token-driven
  components).

## Fixtures

- Mocked `listIntegrations` responses covering all three provider ids in
  both connected and not-connected states.
